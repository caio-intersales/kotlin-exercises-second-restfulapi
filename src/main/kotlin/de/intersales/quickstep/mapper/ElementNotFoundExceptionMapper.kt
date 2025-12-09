package de.intersales.quickstep.mapper

import de.intersales.quickstep.exceptions.ElementNotFoundException
import javax.ws.rs.core.Response
import javax.ws.rs.ext.ExceptionMapper
import javax.ws.rs.ext.Provider

@Provider
class ElementNotFoundExceptionMapper : ExceptionMapper<ElementNotFoundException> {
    override fun toResponse(exception: ElementNotFoundException): Response {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to exception.message))
            .build()
    }
}
