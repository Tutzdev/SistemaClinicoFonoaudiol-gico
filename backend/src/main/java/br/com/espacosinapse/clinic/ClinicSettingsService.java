package br.com.espacosinapse.clinic;

import br.com.espacosinapse.common.ApiDtos.PublicClinicDto;
import br.com.espacosinapse.common.ApiDtos.SettingsDto;
import br.com.espacosinapse.common.ApiDtos.SettingsInput;
import br.com.espacosinapse.common.ApiException;
import br.com.espacosinapse.common.DomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClinicSettingsService {
    public static final UUID SETTINGS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String LEGAL_NAME = "ESPACO SINAPSE CLINICA DE FONOAUDIOLOGIA LTDA";
    private static final String CNPJ = "65.952.229/0001-71";

    private final DomainSupport domainSupport;

    public ClinicSettingsService(DomainSupport domainSupport) {
        this.domainSupport = domainSupport;
    }

    private ClinicSettings entity() {
        return domainSupport.find(ClinicSettings.class, SETTINGS_ID);
    }

    private SettingsDto toDto(ClinicSettings settings) {
        return new SettingsDto(
            settings.displayName,
            settings.description,
            settings.phone,
            settings.email,
            settings.whatsapp,
            settings.publicAddress,
            settings.addressConfirmed,
            settings.version
        );
    }

    public SettingsDto get() {
        return toDto(entity());
    }

    @Transactional
    public SettingsDto update(SettingsInput input) {
        domainSupport.writeLock();

        ClinicSettings settings = entity();
        domainSupport.version(settings, input.version());

        DomainSupport.phone(input.phone(), true);
        DomainSupport.phone(input.whatsapp(), true);
        String publicAddress = DomainSupport.clean(input.publicAddress());
        if (input.addressConfirmed() && publicAddress == null) {
            throw ApiException.bad("Preencha o endereço confirmado de atendimento.");
        }

        settings.displayName = input.displayName().strip();
        settings.description = input.description().strip();
        settings.phone = input.phone().strip();
        settings.email = input.email().strip();
        settings.whatsapp = input.whatsapp().replaceAll("\\D", "");
        settings.publicAddress = publicAddress;
        settings.addressConfirmed = input.addressConfirmed();

        domainSupport.audit("UPDATED", "CLINIC_SETTINGS", settings.id);
        domainSupport.entityManager.flush();

        return toDto(settings);
    }

    public PublicClinicDto published() {
        ClinicSettings settings = entity();
        String publicAddress = settings.addressConfirmed ? settings.publicAddress : null;

        return new PublicClinicDto(
            settings.displayName,
            settings.description,
            settings.phone,
            settings.email,
            settings.whatsapp,
            publicAddress,
            settings.addressConfirmed,
            LEGAL_NAME,
            CNPJ
        );
    }
}
