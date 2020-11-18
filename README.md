## api-gateway-auth

This sample application showcases how to set up and automate different types of authentication supported by 
[Amazon API Gateway HTTP API](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api.html) via [AWS SAM](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html)

- [Mutual TLS](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-mutual-tls.html)
- [JWT authorizers](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-jwt-authorizer.html)
- [AWS Lambda authorizers](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-lambda-authorizer.html)
- [IAM authorization](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-access-control-iam.html) (Not supported via SAM. Ref [issue](https://github.com/aws/aws-sam-cli/issues/2233))]

This SAM app uses java as language runtime for the lambda functions and custom resources.

# Setup

The main SAM [template.yaml](template.yaml) is used to set up HTTP API and different types of auth mentioned above.
As a pre requisite step, in order to configure JWT authorizer, you will need to run [template-cognito.yaml](template-cognito.yaml)
to setup [Amazon Cognito](https://aws.amazon.com/cognito/) as the JWT token provider. Lets begin.

### Setup JWT Token provider

This will end up creating cognito user pool which we will use to set up our HTTP API with different auths.
This is needed because we will use [Amazon Cognito](https://aws.amazon.com/cognito/) as the JWT token provider.
You can skip this step if you are not going to configure JWT Authorizer for your HTTP API 
in [template.yaml](template.yaml#L174)

```
    api-gateway-auth$ sam build -t template-cognito.yaml
```

```
    api-gateway-auth$ sam deploy -t template-cognito.yaml -g

    Deploying with following values
    ===============================
    Stack name                 : jwt-auth
    Region                     : eu-west-1
    Confirm changeset          : False
    Deployment s3 bucket       : aws-sam-cli-managed-default-samclisourcebucket-randomhash
    Capabilities               : ["CAPABILITY_IAM"]
    Parameter overrides        : {'AppName': 'jwt-auth', 'ClientDomains': 'http://localhost:8080', 'AdminEmail': 'emailaddress@example.com', 'AddGroupsToScopes': 'true'}

```

### Set up HTTP API

```
    api-gateway-auth$ sam build
```

```
    api-gateway-auth$ sam deploy

    Deploying with following values
    ===============================
    Stack name                 : http-api-authdemo
    Region                     : eu-west-1
    Confirm changeset          : False
    Deployment s3 bucket       : aws-sam-cli-managed-default-samclisourcebucket-randomhash
    Capabilities               : ["CAPABILITY_IAM"]
    Parameter overrides        : {'UserPoolId': 'from previous stack output', 'Audience': 'from previous stack output', 'HostedZoneId': 'Hosted zone id for custom domain', 'DomainName': 'domain name for the http api', 'TruststoreKey': 'truststore.pem'}
```

## Testing and validation

At this point, your stack should update successfully and you will have a HTTP API with Mutual TLS setup by default using 
[AWS Certificate Manager Private Certificate Authority](https://aws.amazon.com/certificate-manager/private-certificate-authority/).

Stack will also generate one of the client certificates for you to validate the API.

- Navigate to [AWS Certificate Manager console](https://console.aws.amazon.com/acm/home). You will find a private 
certificate already generated with Name as ClientOneCert.

- Select the cert and under action choose `Export (Private certificates only)`. Enter passphrase on next screen which 
will be needed to decrypt the `Certificate private key` later.

- `Export certificate body to a file` and `Export certificate private key to a file`

- Decrypt private key downloaded using below command:

```
    openssl rsa -in <<Encrypted file>> -out client.decrypted.txt

    Enter pass phrase for client.encrypted.txt:
    writing RSA key
```

- Now you should be able to access the configured api with different paths and auth methods using mutual TLS.

```
    curl -v --cert client.pem  --key client.decrypted.txt https://<<api-auth-demo.domain.com>>
```


##


## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This library is licensed under the MIT-0 License. See the LICENSE file.

