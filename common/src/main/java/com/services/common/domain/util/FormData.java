package com.services.common.domain.util;

import org.jboss.resteasy.reactive.PartType;

import javax.validation.constraints.NotNull;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.MediaType;
import java.io.File;

public class FormData {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @FormParam("filename")
    @PartType(MediaType.TEXT_PLAIN)
    @NotNull
    public String fileName;

    @FormParam("mimetype")
    @PartType(MediaType.TEXT_PLAIN)
    @NotNull
    public String mimeType;
}

