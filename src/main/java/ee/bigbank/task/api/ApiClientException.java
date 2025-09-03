package ee.bigbank.task.api;

/**
 * Exception thrown when there is an error with the API client.
 */
public class ApiClientException extends RuntimeException {
    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
