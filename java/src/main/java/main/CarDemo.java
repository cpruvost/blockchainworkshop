package main;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.security.SecureRandom;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.NetworkConfig.CAInfo;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.yaml.snakeyaml.Yaml;
import static java.nio.charset.StandardCharsets.UTF_8;

public class CarDemo {
	 private static  String CHANNEL_NAME = ConfigProperties.values("CHANNEL_NAME");
	
	 private static  String ENDORSEMENT_POLICY_PATH = ConfigProperties.values("ENDORSEMENT_POLICY_PATH");
	 private static  String SAMPLE_FILE_PATH = ConfigProperties.values("SAMPLE_FILE_PATH");
	 private static  String CHAIN_CODE_NAME = ConfigProperties.values("CHAIN_CODE_NAME");
	 private static  String CHAIN_CODE_PATH = ConfigProperties.values("CHAIN_CODE_PATH");
	 private static  String CHAIN_CODE_VERSION = ConfigProperties.values("CHAIN_CODE_VERSION");
	 private static  String CHAIN_CODE_TYPE = ConfigProperties.values("CHAIN_CODE_TYPE");
	 private static  String ENROLL_SWITCH = ConfigProperties.values("ENROLL_SWITCH");
	 private static  String USER_NAME = ConfigProperties.values("USER_NAME");
	 private static  String USER_PWD = ConfigProperties.values("USER_PWD");
	 private static  String CHAIN_CODE_FILEPATH = ConfigProperties.values("CHAIN_CODE_FILEPATH");
	 private static  String TARGET_PEER = ConfigProperties.values("TARGET");

	 private static  NetworkConfig networkConfig;
	 private static  Type 	CHAIN_CODE_LANG = Type.GO_LANG;
	 private static  Collection<Peer> targets = new LinkedList<>();

	public static void main(String[] args) {
		FileInputStream fis = null; 
		 try {
			 	//Get the artifacts root path
			 	Path basepath = Paths.get("..").normalize().toRealPath();
				out("Base Path is : " + basepath.toString());

				//Chaincode language check
				if(CHAIN_CODE_TYPE.equals("node")){
					CHAIN_CODE_LANG = Type.NODE;	
				}

			    // Use the appropriate TLS network config file
			    networkConfig = NetworkConfig.fromYamlFile(new File(basepath.toString(),"/network.yaml"));

			    ///////////////
			 	/// Get clientInfo from config file
				Yaml yaml = new Yaml();
				fis = new FileInputStream(basepath.toString()+"/network.yaml");
			 	Map yamlMap = yaml.load(fis);
				Map clientInfo = (Map)yamlMap.get("client");
				String orgName = clientInfo.get("organization").toString();

				///////////////
		        /// Create instance of fabric client then log in.
		        HFClient client = HFClient.createNewInstance();
		        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
		        client.setUserContext(networkConfig.getPeerAdmin(orgName));

		        ///////////////
		        /// Connect to the channel 
		        Channel channel = reconstructChannel(CHANNEL_NAME, client);
		        
		    		///////////////
		        /// Install carTrace go chaincode
			/**
		        ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
		    	        .setVersion(CHAIN_CODE_VERSION)
		    	        .setPath(CHAIN_CODE_PATH).build();
			**/
		        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
	                        .setVersion(CHAIN_CODE_VERSION);

		        if(CHAIN_CODE_LANG == Type.GO_LANG){
		            chaincodeIDBuilder.setPath(Paths.get(CHAIN_CODE_PATH, "go").toString());
		        }else{
		            //chaincode path must be null for node, sdk find the chaincode based on ChaincodeSourceLocation
		            out("chaincodeType is node, set chaincode path to null.");
		            chaincodeIDBuilder.setPath("");
		        }
		        ChaincodeID chaincodeID = chaincodeIDBuilder.build();
		        
		        installChaincode(client, channel, chaincodeID, basepath);
		        
		        ///////////////
		        /// From when you install + instantiate the chaincode, the orderer -> peer update could be pending
		        /// We put a minor sleep here before executing the chaincode. Ideally we would 
		        /// listen to events from the orderer.
		        Thread.sleep(60000);

				//////////////
				/// Enroll user if ENROLL_SWITCH equals true
				if(ENROLL_SWITCH.equals("true")){
					enrollUser(client,orgName);
				}
		        
		        // With the chaincode installed & instantated (a one time event bar chaincode updates),
		        // we can now invoke the chaincode.
		        
		        	///////////////
				/// Send transaction proposal to all peers Invoke chaincode to create a new vehicle part
				String newPart = newVehicleNum("ser");
				String newCar = newVehicleNum("mer");
		        CompletableFuture<BlockEvent.TransactionEvent> partFuture =  createPart(client, channel, chaincodeID,
		        		newPart, "tasa", 1502688979, "airbag 2020", "aaimler ag / mercedes", false, 0); 

				BlockEvent.TransactionEvent transactionEvent_1 = partFuture.get(60000, TimeUnit.SECONDS);
				if (! transactionEvent_1.isValid()) {
    				out("partFuture: Transaction tx: " + transactionEvent_1.getTransactionID() + " is invalid.");
					System.exit(-1);
				}

		    		///////////////
		        /// Invoke chaincode to create a new vehicle
		        CompletableFuture<BlockEvent.TransactionEvent> carfuture =  createCar(client, channel, chaincodeID,
						newCar, "mercedes", "c class", 1502688979, newPart, "mercedes", false, 0);

				BlockEvent.TransactionEvent transactionEvent_2 = carfuture.get(60000, TimeUnit.SECONDS);
				if (!transactionEvent_2.isValid()) {
    				out("carfuture: Transaction tx: " + transactionEvent_2.getTransactionID() + " is invalid.");
					System.exit(-1);
				}

		        //Sometimes it report table in bdb is not existed, actually it needs time to create
		        //Thread.sleep(60000);

		        ///////////////
		        /// Invoke chaincode to query the vehicle part by rich query
		        int ret =  queryPartByOwner(client, channel, chaincodeID,
		        		"aaimler ag / mercedes");

				if (ret == -1) {
					out("query result is not as expected.");
					System.exit(-1);
				}

				out("All Done");  
				System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if(fis != null){
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static String newVehicleNum(String verType){
		SecureRandom ran = new SecureRandom();
		return verType + Integer.toString(ran.nextInt(100000));
	}
	
	// connect to channel and add the peer(s) we wish to execute transactions with
	static private Channel reconstructChannel(String name, HFClient client) throws InvalidArgumentException,NetworkConfigurationException,TransactionException {
        out("Reconstructing %s channel", name);        
        
        // Instantiate channel
		Channel channel = client.loadChannelFromConfig(name, networkConfig);
        channel.initialize();
      
        out("Finished reconstructing channel %s.", name);
        
        return channel;
    }

	// install & instantiate chaincode
	static void installChaincode(HFClient client, Channel channel, ChaincodeID chaincodeID, Path basePath) {
		
		////////////////////////////
        // Install Proposal Request
        //
        Collection<Orderer> orderers = channel.getOrderers();
        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        
        try
        {        			
			out("Creating install proposal");
			
			InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
			installProposalRequest.setChaincodeID(chaincodeID);
			
			////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
			if(CHAIN_CODE_LANG == Type.GO_LANG){
				installProposalRequest.setChaincodeSourceLocation(basePath.resolve(SAMPLE_FILE_PATH).toFile());
			} else {
				installProposalRequest.setChaincodeSourceLocation(basePath.resolve(CHAIN_CODE_FILEPATH).toFile());
			}
			installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);
			installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
			
			out("Sending install proposal");
			
			////////////////////////////
			// only a client from the same org as the peer can issue an install request
			int numInstallProposal = 0;
			
			Collection<Peer> peers = channel.getPeers();
			for (Peer peer : peers) {
				if (TARGET_PEER != null && TARGET_PEER.length () != 0 ) {
					//out("checking if %s is %s", peer.getName(), TARGET_PEER);
					if ( peer.getName().compareTo(TARGET_PEER) == 0 ) {
						out("Adding %s", peer.getName());
						targets.add(peer);
						break;
					}
		
				} else {
					//only select one peer
					out("select peer: %s", peer.getName());
					targets.add(peer);
					break;
				}
				
			} 

			numInstallProposal = numInstallProposal + targets.size();
			responses = client.sendInstallProposal(installProposalRequest, targets);
			
			for (ProposalResponse response : responses) {
			    if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
			        out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
			        successful.add(response);
			    } else {
			        failed.add(response);
			    }
			}
			
			out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());
			
			if (failed.size() > 0) {
				ProposalResponse first = failed.iterator().next();
				if(first.getMessage().contains("exists")){
			    	out("Chaincode exists. Continue...");
			    }
                else out("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
			}
			
			//// Instantiate chaincode.
			InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
			instantiateProposalRequest.setProposalWaitTime(120000);//time in milliseconds
			instantiateProposalRequest.setChaincodeID(chaincodeID);
			instantiateProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
			// vehicleTrace chaincode does not utilize parameters in its "init" method to 
			// setup the ledger for the first time
			instantiateProposalRequest.setFcn("init");
			instantiateProposalRequest.setArgs(new String[] {""});
			Map<String, byte[]> tm = new HashMap<>();
			tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
			tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
			instantiateProposalRequest.setTransientMap(tm);
			
			/*
			    Load endorsement policy from yaml file
			    See fabric-sdk-java/README.md Chaincode endorsement policies section for more details.
			  */
		  ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
		  chaincodeEndorsementPolicy.fromYamlFile(new File(ENDORSEMENT_POLICY_PATH + "/chaincode-endorsement-policy.yaml"));
		  instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
	       successful.clear();
           failed.clear();

           responses = channel.sendInstantiationProposal(instantiateProposalRequest, targets);
               
           for (ProposalResponse response : responses) {
               if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                   successful.add(response);
                   out("Succesful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
               } else {
                   failed.add(response);
               }
           }
           out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
		  Boolean isExist = false;   
		   if (failed.size() > 0) {
               for (ProposalResponse fail : failed) {
				if(fail.getMessage().contains("exists")){
			    	out("Chaincode exists. Continue...");
			    }
                else out("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + fail.getMessage() + ", on peer" + fail.getPeer());

               }
			   ProposalResponse first = failed.iterator().next();
			   if(first.getMessage().contains("exists")){
					out("Chaincode exists. Continue...");
					isExist = true;
				}
				else out("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
           }
           
           ///////////////
           /// Send instantiate transaction to orderer
           out("Sending instantiateTransaction to orderer");
		   if(!isExist) channel.sendTransaction(successful, orderers);
        } catch (CompletionException e) {
            throw e;
        } catch (Exception e) {
            throw new CompletionException(e);
        }
	}

	//Enroll a user
	static void enrollUser(HFClient client, String orgName){
		out("Start enroll user: "+USER_NAME);
		try{
 			NetworkConfig.OrgInfo org = networkConfig.getOrganizationInfo(orgName);
			CAInfo caInfo = org.getCertificateAuthorities().get(0);
			HFCAClient hfcaClient = HFCAClient.createNewInstance(caInfo);
			ObcsUser obcsUser = new ObcsUser(USER_NAME,orgName);
			obcsUser.setEnrollmentSecret(USER_PWD);
			obcsUser.setEnrollment(hfcaClient.enroll(obcsUser.getName(), obcsUser.getEnrollmentSecret()));
			client.setUserContext(obcsUser);
		}catch (Exception e) {
			throw new CompletionException(e);
		}
		out("Enroll user success");
	}

	// invoke chaincode's initVehicle method to create a new car
	static CompletableFuture<BlockEvent.TransactionEvent> createCar(HFClient client, Channel channel, ChaincodeID chaincodeID,
															String chassisNumber,
															String manufacturer,
															String model,
															int assemblyDate,
															String airbagSerialNumber,
															String owner,
															Boolean recall,
															int recallDate) {

		String[] args = {chassisNumber, manufacturer, model, String.valueOf(assemblyDate), airbagSerialNumber, owner, String.valueOf(recall), String.valueOf(recallDate)};
		return invokeChainCode(client, channel,chaincodeID,  "initVehicle", args);
	}
	
	// invoke chaincode's initVehiclePart method to create a new car part
	static CompletableFuture<BlockEvent.TransactionEvent> createPart(HFClient client, Channel channel, ChaincodeID chaincodeID,
															String serialNumber,
															String assembler,
															int assemblyDate,
															String name,
															String owner,
															Boolean recall,
															int recallDate) {

		String[] args = {serialNumber, assembler, String.valueOf(assemblyDate), name, owner, String.valueOf(recall), String.valueOf(recallDate)};
		return invokeChainCode(client, channel, chaincodeID, "initVehiclePart", args);
	}

	// query a vehiclePart by rich query with query param "Owner"
	static int queryPartByOwner(HFClient client, Channel channel, ChaincodeID chaincodeID,
															String owner) {

		String[] args = {owner};
		String payload = queryChainCode(client, channel, chaincodeID, "queryVehiclePartByOwner", args);
		if (payload.length() == 0){
			return -1;
		}
		return 0;
	}

	static String queryChainCode(HFClient client, Channel channel, ChaincodeID chaincodeID, String method, String [] args) {

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn("queryVehiclePartByOwner");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = channel.queryByChaincode(queryByChaincodeRequest, targets);
        } catch (Exception e) {
            throw new CompletionException(e);
        }

        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != Status.SUCCESS) {
                out("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                        ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
				return "";
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
				return payload;
            }
        }
		return "";
	}
	
	// invoke chaincode's initVehiclePart method to create a new car
	static CompletableFuture<BlockEvent.TransactionEvent> invokeChainCode(HFClient client, Channel channel, ChaincodeID chaincodeID, String method, String [] args) {

	        try {
	            Collection<ProposalResponse> successful = new LinkedList<>();
	            Collection<ProposalResponse> failed = new LinkedList<>();

	            ///////////////
	            /// Send transaction proposal to all peers
	            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
	            transactionProposalRequest.setChaincodeID(chaincodeID);
	            transactionProposalRequest.setFcn(method);
	            transactionProposalRequest.setArgs(args);
	            //transactionProposalRequest.setProposalWaitTime(time in milliseconds);
	            
	            out("sending %s transaction proposal to all peers", method);

	            Collection<ProposalResponse> invokePropResp = channel.sendTransactionProposal(transactionProposalRequest, targets);
	            for (ProposalResponse response : invokePropResp) {
	                if (response.getStatus() == Status.SUCCESS) {
	                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
	                    successful.add(response);
	                } else {
	                    failed.add(response);
	                }
	            }

	            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
	                    invokePropResp.size(), successful.size(), failed.size());
	            
	            if (failed.size() > 0) {
	                ProposalResponse firstTransactionProposalResponse = failed.iterator().next();

	                throw new ProposalException(format("Not enough endorsers for invoke :%d endorser error:%s. Was verified:%b",
	                        firstTransactionProposalResponse.getStatus().getStatus(), firstTransactionProposalResponse.getMessage(), firstTransactionProposalResponse.isVerified()));
				}
	            
	            out("Successfully received %s transaction proposal responses.", method);

	            ////////////////////////////
	            // Send transaction to orderer
	            out("Sending chaincode transaction");
	            return channel.sendTransaction(successful);
	        } catch (Exception e) {
	            throw new CompletionException(e);
	        }
	}

    static void out(String format, Object... args) {
        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();
    }

}
