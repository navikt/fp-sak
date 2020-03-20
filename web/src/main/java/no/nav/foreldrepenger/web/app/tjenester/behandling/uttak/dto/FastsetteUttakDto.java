package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

public abstract class FastsetteUttakDto extends BekreftetAksjonspunktDto {

    @Valid
    @NotNull
    @Size(min = 1, max = 1500)
    private List<UttakResultatPeriodeLagreDto> perioder;

    FastsetteUttakDto() { //NOSONAR
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

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakPerioderDto() {
            // For Jackson
        }

        public FastsetteUttakPerioderDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.TILKNYTTET_STORTINGET_KODE)
    public static class FastsetteUttakTilknyttetStortinget extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakTilknyttetStortinget() {
            // For Jackson
        }

        public FastsetteUttakTilknyttetStortinget(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }


    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE_KODE)
    public static class FastsetteUttakKontrollerRealitetsBehandlingEllerKlageDto extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakKontrollerRealitetsBehandlingEllerKlageDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerRealitetsBehandlingEllerKlageDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_FORDELING_AV_STØNADSPERIODEN_KODE)
    public static class FastsetteUttakKontrollerOpplysningerOmFordelingAvStønadsperiodenDto extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakKontrollerOpplysningerOmFordelingAvStønadsperiodenDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerOpplysningerOmFordelingAvStønadsperiodenDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD_KODE)
    public static class FastsetteUttakKontrollerOpplysningerOmDødDto extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakKontrollerOpplysningerOmDødDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerOpplysningerOmDødDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE)
    public static class FastsetteUttakKontrollerOpplysningerOmSøknadsfristDto extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakKontrollerOpplysningerOmSøknadsfristDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerOpplysningerOmSøknadsfristDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_TILSTØTENDE_YTELSER_INNVILGET_KODE)
    public static class FastsetteUttakKontrollerTilstøtendeYtelserInnvilgetDto extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakKontrollerTilstøtendeYtelserInnvilgetDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerTilstøtendeYtelserInnvilgetDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }

    }

    @JsonTypeName(AksjonspunktKodeDefinisjon.KONTROLLER_TILSTØTENDE_YTELSER_OPPHØRT_KODE)
    public static class FastsetteUttakKontrollerTilstøtendeYtelserOpphørtDto extends FastsetteUttakDto {

        @SuppressWarnings("unused") // NOSONAR
        private FastsetteUttakKontrollerTilstøtendeYtelserOpphørtDto() {
            // For Jackson
        }

        public FastsetteUttakKontrollerTilstøtendeYtelserOpphørtDto(List<UttakResultatPeriodeLagreDto> perioder) {
            super(perioder);
        }


    }
}
