package com.teamflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException() {
        super("You do not have permission to perform this action on the project.");
    }
}
