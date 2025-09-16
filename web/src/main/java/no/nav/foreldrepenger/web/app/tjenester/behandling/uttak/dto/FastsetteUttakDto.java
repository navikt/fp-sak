package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

public abstract class FastsetteUttakDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1, max = 1500)
    private List<UttakResultatPeriodeLagreDto> perioder;

    FastsetteUttakDto() {
        // jackson
    }

    public FastsetteUttakDto(List<UttakResultatPeriodeLagreDto> perioder) {
        this.perioder = perioder;
    }

    public List<UttakResultatPeriodeLagreDto> getPerioder() {
        return perioder;
    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_UTTAKPERIODER_KODE)
    public static class FastsetteUttakPerioderDto extends FastsetteUttakDto {

        @SuppressWarnings("unused")
        private FastsetteUttakPerioderDto() {
            // For Jackson
        }

        public FastsetteUttakPerioderDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_UTTAK_STORTINGSREPRESENTANT_KODE)
    public static class FastsetteUttakStortingsrepresentantDto extends FastsetteUttakDto {

        @SuppressWarnings("unused")
        private FastsetteUttakStortingsrepresentantDto() {
            // For Jackson
        }

        public FastsetteUttakStortingsrepresentantDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @Deprecated // TODO: TFP-6302: Opprydding
    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_ANNENPART_EØS_KODE)
    public static class FastsetteUttakKontrollerAnnenpartEØSDto extends FastsetteUttakDto {

        @SuppressWarnings("unused")
        private FastsetteUttakKontrollerAnnenpartEØSDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerAnnenpartEØSDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }


    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE_KODE)
    public static class FastsetteUttakKontrollerRealitetsBehandlingEllerKlageDto extends FastsetteUttakDto {

        @SuppressWarnings("unused")
        private FastsetteUttakKontrollerRealitetsBehandlingEllerKlageDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerRealitetsBehandlingEllerKlageDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD_KODE)
    public static class FastsetteUttakKontrollerOpplysningerOmDødDto extends FastsetteUttakDto {

        @SuppressWarnings("unused")
        private FastsetteUttakKontrollerOpplysningerOmDødDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerOpplysningerOmDødDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE)
    public static class FastsetteUttakKontrollerOpplysningerOmSøknadsfristDto extends FastsetteUttakDto {

        @SuppressWarnings("unused")
        private FastsetteUttakKontrollerOpplysningerOmSøknadsfristDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerOpplysningerOmSøknadsfristDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }
}
