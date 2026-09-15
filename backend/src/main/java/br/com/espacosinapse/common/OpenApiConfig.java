package br.com.espacosinapse.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {
    private static final Set<PathItem.HttpMethod> WRITE_METHODS = Set.of(
        PathItem.HttpMethod.POST,
        PathItem.HttpMethod.PUT,
        PathItem.HttpMethod.PATCH,
        PathItem.HttpMethod.DELETE
    );

    @Bean
    OpenAPI clinicApi() {
        Info info = new Info()
            .title("Espaço Sinapse — API administrativa")
            .version("1.0")
            .description(
                "Sessão HttpOnly. Obtenha GET /api/v1/auth/csrf e envie seu token no headerName "
                    + "retornado em operações de escrita. Após login, obtenha novo CSRF. ADMIN "
                    + "administra cadastros; RECEPCAO gerencia pacientes e agenda. Datas usam offset "
                    + "ISO-8601. Atualizações exigem version. Erros usam ProblemDetail; conflitos "
                    + "retornam 409, incluindo conflictingAppointmentIds quando aplicável. API "
                    + "pública expõe somente campos aprovados."
            );
        SecurityScheme sessionCookie = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.COOKIE)
            .name("JSESSIONID");

        return new OpenAPI()
            .info(info)
            .components(new Components().addSecuritySchemes("sessionCookie", sessionCookie))
            .addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
    }

    @Bean
    OpenApiCustomizer operationSecurityContract() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean publicRead = method == PathItem.HttpMethod.GET
                && (path.startsWith("/api/v1/public/")
                    || Set.of("/api/v1/auth/csrf", "/api/v1/health").contains(path));
            boolean login = method == PathItem.HttpMethod.POST && path.equals("/api/v1/auth/login");
            if (publicRead || login) {
                // An anonymous CSRF session is not an authenticated sessionCookie requirement.
                operation.setSecurity(List.of());
            }
            if (WRITE_METHODS.contains(method)) {
                Parameter csrfHeader = new Parameter()
                    .in("header")
                    .name("X-CSRF-TOKEN")
                    .required(true)
                    .schema(new StringSchema())
                    .description(
                        "Token retornado por GET /api/v1/auth/csrf. Conserve o cookie da mesma "
                            + "sessão; obtenha um novo token após login."
                    );
                operation.addParametersItem(csrfHeader);
            }
            if (login) {
                operation.setDescription(
                    "Entrada pública por e-mail e senha; não exige sessão previamente autenticada. "
                        + "Antes de enviar, consulte GET /api/v1/auth/csrf, conserve o cookie de sessão "
                        + "anônima e envie X-CSRF-TOKEN. Após sucesso, a sessão fica autenticada e o "
                        + "CSRF anterior é invalidado; consulte /auth/csrf novamente. CSRF ausente ou "
                        + "inválido retorna 403; credenciais inválidas, 401; limite de tentativas "
                        + "excedido, 429."
                );
            }
        }));
    }
}
