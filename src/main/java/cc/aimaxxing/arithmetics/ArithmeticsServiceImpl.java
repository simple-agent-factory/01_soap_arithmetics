package cc.aimaxxing.arithmetics;

import jakarta.jws.WebService;
import jakarta.xml.soap.SOAPFactory;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;

import javax.xml.namespace.QName;

@WebService(
    endpointInterface = "cc.aimaxxing.arithmetics.ArithmeticsService",
    serviceName = "ArithmeticsService",
    portName = "ArithmeticsPort",
    targetNamespace = "http://arithmetics.aimaxxing.cc/"
)
public class ArithmeticsServiceImpl implements ArithmeticsService {

    @Override
    public int add(int a, int b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            throw soapFault("Integer overflow in add(" + a + ", " + b + ")");
        }
    }

    @Override
    public int subtract(int a, int b) {
        try {
            return Math.subtractExact(a, b);
        } catch (ArithmeticException e) {
            throw soapFault("Integer overflow in subtract(" + a + ", " + b + ")");
        }
    }

    @Override
    public int multiply(int a, int b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            throw soapFault("Integer overflow in multiply(" + a + ", " + b + ")");
        }
    }

    @Override
    public int divide(int a, int b) {
        // BUG: missing zero-check — throws raw ArithmeticException instead of SOAP Fault
        return a / b;
    }

    private SOAPFaultException soapFault(String message) {
        try {
            SOAPFactory factory = SOAPFactory.newInstance();
            // SOAP 1.1 fault code: Server indicates a server-side processing error
            SOAPFault fault = factory.createFault(
                message,
                new QName("http://schemas.xmlsoap.org/soap/envelope/", "Server")
            );
            return new SOAPFaultException(fault);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create SOAP fault: " + message, e);
        }
    }
}
