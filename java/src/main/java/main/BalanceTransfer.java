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

public class BalanceTransfer {
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
            System.out.println("orgname : " + orgName);

            ///////////////
            /// Create instance of fabric client then log in.
            HFClient client = HFClient.createNewInstance();
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            client.setUserContext(networkConfig.getPeerAdmin(orgName));
            System.out.println("client : " + client.getUserContext().getName());

            /// Connect to the channel 
            Channel channel = reconstructChannel(CHANNEL_NAME, client);   
            System.out.println("channel : " + channel.getName()); 

            ///get Peers
            getPeers(channel);
            
            ///getChaincodeID
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
            System.out.println(chaincodeID);


            ///query
            queryBalanceByOwner(client, channel, chaincodeID, "a");

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

   static void out(String format, Object... args) {
        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();
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

    //get peers
    static private void getPeers(Channel channel) {
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
    }

    ///querychaincode
    static String queryChainCode(HFClient client, Channel channel, ChaincodeID chaincodeID, String method, String [] args) {

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn("query");
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
    
    // query a vehiclePart by rich query with query param "Owner"
	static int queryBalanceByOwner(HFClient client, Channel channel, ChaincodeID chaincodeID, String owner) {

        String[] args = {owner};
        String payload = queryChainCode(client, channel, chaincodeID, "query", args);
        if (payload.length() == 0){
            return -1;
        }
        System.out.println(payload);
        return 0;
    }

}