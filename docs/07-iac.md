# Use Infra As Code to create Oracle Blockchain Network (Under Construction)

The first step is to install the Paas Service Manager CLI (psm CLI)

First open your cloud console in order to find the name of your identity cloud service (ex for me : idcs-40e2d2239516421bb71b0bb09597c4fa)

In the menu click on Identity / Federation and note the name of your identity Cloud service

![identity cloud service](images/07-iac.png)

Then you need to download the psmcli.zip with curl. Replace the idcs name by yours and be carefull if you are not in europe because in this case you have to update the psm url too.

The Identify your REST API server name:

- If you log in to your Oracle cloud account with a US data center, use `psm.us.oraclecloud.com`
- If you log in to your Oracle cloud account with the aucom region, use `psm.aucom.oraclecloud.com`
- Otherwise, use `psm.europe.oraclecloud.com`

```
curl -X GET -u "myuser321:password -H X-ID-TENANT-NAME:"idcs-40e2d2239516421bb71b0bb09597c4fa" "https://psm.europe.oraclecloud.com/paas/api/v1.1/cli/idcs-40e2d2239516421bb71b0bb09597c4fa/client" -o psmcli.zip
```

Then install psmcli.

```
On Linux : sudo —H pip3 install -U psmcli.zip
On Windows and ubuntu : pip3 install -U psmcli.zip
```

Update your PATH variable to be able to call psm everywhere

```
sboxes@osboxes:~/Desktop$ psm --version
Oracle PaaS CLI client 
Version 1.1.28
```

Now you have to do a psm setup before using it. In this step you need the idcs name.

```
osboxes@osboxes:~/Desktop$ psm setup
Username: christophe.pruvost@oracle.com
Password: 
Retype Password: 
Identity domain: idcs-40e2d2239516421bb71b0bb09597c4fa
Region [us]: emea
Output format [short]: 
Use OAuth?[n]
-------------------------------------------
'psm setup' was successful. Available services are:

....
OABCSINST : Oracle Blockchain Platform
....
```

Do psm OABCSINST in order to explore all the available commands

```
osboxes@osboxes:~/Desktop$ psm OABCSINST help

DESCRIPTION
  Oracle Blockchain Platform

SYNOPSIS
  psm OABCSINST <command> [parameters]

AVAILABLE COMMANDS
  o services
       List all Oracle Blockchain Platform instances
  o service
       List Oracle Blockchain Platform instance
  o create-service
       The create service operation
  o delete-service
       Delete Service
  o check-health
       Checks health of the service resources
  o operation-status
       View status of Oracle Blockchain Platform instance operation
  o activities
       View activities for Oracle Blockchain Platform instance
  o help
       Show help
```
