package cc.aimaxxing.arithmetics;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(
    name = "ArithmeticsService",
    targetNamespace = "http://arithmetics.aimaxxing.cc/"
)
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public interface ArithmeticsService {

    @WebMethod
    @WebResult(name = "result")
    int add(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );

    @WebMethod
    @WebResult(name = "result")
    int subtract(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );

    @WebMethod
    @WebResult(name = "result")
    int multiply(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );

    @WebMethod
    @WebResult(name = "result")
    int divide(
        @WebParam(name = "a") int a,
        @WebParam(name = "b") int b
    );
}
