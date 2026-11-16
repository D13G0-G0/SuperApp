package com.superapp.api.shared.security;

/**
 * Da el id del usuario que hace la petición.
 * Los módulos dependen solo de esta interfaz: así se puede cambiar
 * cómo se identifica al usuario (header de prueba, JWT...) sin tocarlos.
 */
public interface CurrentUser {

    /** Id del usuario autenticado. Lanza UnauthorizedException si no hay sesión. */
    Long id();
}
