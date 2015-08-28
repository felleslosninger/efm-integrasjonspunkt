package soapui;

/**
 * Note: currently does not work
 */
public class SoapUiTestFromCodeExample {

    public static void main(String[] args) {

    }
    /*
    public static void main(String[] args) throws XmlException, IOException, SoapUIException, Request.SubmitException {
        String[] properties = {"orgnr=" + 974720760};

        WsdlProject wsdlProject = new WsdlProject();
        WsdlInterface wsdlInterface = WsdlInterfaceFactory.importWsdl(wsdlProject, "http://localhost:9091/integrasjonspunkt/noarkExchange?wsdl", true)[0];
        WsdlOperation operation = (WsdlOperation) wsdlInterface.getOperationByName("GetCanReceiveMessage");
        WsdlRequest request = operation.addNewRequest( "My request" );

        request.setRequestContent( operation.createRequest( true ) );
        WsdlSubmit submit = (WsdlSubmit) request.submit( new WsdlSubmitContext(request), false );

        Response response = submit.getResponse();
        String content = response.getContentAsString();
        System.out.println( content );
        assertNotNull( content );
        assertTrue( content.indexOf( "404 Not Found" ) > 0  );
    }
    */
}
