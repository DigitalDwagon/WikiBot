package dev.digitaldragon.interfaces;

/**
 * UserErrorException is an exception class that is used to indicate errors caused by the user.
 * This is helpful for calling methods to check user permissions, as it allows the method to
 * halt execution and return a user-friendly error message without the parent method having to
 * run any additional code.
 *
 *<p>
 *     If this exception is thrown, the message should be shown directly to the user with no
 *     modifications. You are free to throw this exception in any interface method, but it is
 *     not meant to be used in any other context.
 *</p>
 */
public class UserErrorException extends Exception {

    public UserErrorException(String message) {
        super(message);
    }
}
