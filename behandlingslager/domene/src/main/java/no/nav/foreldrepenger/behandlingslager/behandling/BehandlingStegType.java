package no.nav.foreldrepenger.behandlingslager.behandling;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus.UTREDES;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum BehandlingStegType implements Kodeverdi {

    // Steg koder som deles av alle ytelser
    REGISTRER_SØKNAD("REGSØK", "Registrer søknad", UTREDES),
    INNHENT_SØKNADOPP("INSØK", "Innhent søknadsopplysninger", UTREDES),
    VURDER_KOMPLETTHET("VURDERKOMPLETT", "Vurder kompletthet", UTREDES),
    INNHENT_REGISTEROPP("INREG", "Innhent registeropplysninger - innledende oppgaver", UTREDES),
    INREG_AVSL("INREG_AVSL", "Innhent registeropplysninger - resterende oppgaver", UTREDES),
    KONTROLLER_FAKTA("KOFAK", "Kontroller Fakta", UTREDES),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT("VURDEROP", "Kontrollerer søkers opplysningsplikt", UTREDES),
    VURDER_MEDLEMSKAPVILKÅR("VURDERMV", "Vurder medlemskapvilkår", UTREDES),
    VURDER_SAMLET("VURDERSAMLET", "Vurder vilkår samlet", UTREDES),
    BEREGN_YTELSE("BERYT", "Beregn ytelse", UTREDES),
    FORESLÅ_BEHANDLINGSRESULTAT("FORBRES", "Foreslå behandlingsresultat", UTREDES),
    SIMULER_OPPDRAG("SIMOPP", "Simuler oppdrag", UTREDES),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler", UTREDES),
    FORESLÅ_VEDTAK("FORVEDSTEG", "Foreslå vedtak", UTREDES),
    FATTE_VEDTAK("FVEDSTEG", "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    IVERKSETT_VEDTAK("IVEDSTEG", "Iverksett Vedtak", IVERKSETTER_VEDTAK),

    KLAGE_VURDER_FORMKRAV_NFP("VURDER_FK_UI", "Vurder formkrav (NFP)", UTREDES),
    KLAGE_NFP("KLAGEUI", "Vurder Klage (NFP)", UTREDES),
    KLAGE_VURDER_FORMKRAV_NK("VURDER_FK_OI", "Vurder formkrav (NK)", UTREDES),
    KLAGE_NK("KLAGEOI", "Vurder Klage (NK)", UTREDES),
    ANKE("ANKE", "Vurder anke", UTREDES),
    ANKE_MERKNADER("ANKE_MERKNADER", "Vurder merknader", UTREDES),
    VURDER_INNSYN("VURDINNSYN", "Vurder innsynskrav", UTREDES),
    INNHENT_PERSONOPPLYSNINGER("INPER", "Innhent personopplysninger", UTREDES),


    // Kun for Engangsstønad
    VURDER_SØKNADSFRISTVILKÅR("VURDERSFV", "Vurder felles inngangsvilkår", UTREDES),

    // Kun for Foreldrepenger
    DEKNINGSGRAD("DEKNINGSGRAD", "Fastsetter dekningsgrad for behandling", UTREDES),
    FORESLÅ_BESTEBEREGNING("FORS_BESTEBEREGNING", "Foreslå besteberegning", UTREDES),
    KONTROLLER_OMSORG_RETT("KONTROLLER_OMSORG_RETT", "Kontroller aleneomsorg og rett", UTREDES),
    VULOMED("VULOMED", "Vurder løpende medlemskap", UTREDES), // Hvorfor kun FP ?
    FAKTA_LØPENDE_OMSORG("FAKTA_LØPENDE_OMSORG", "Fakta om omsorg", UTREDES),
    GRUNNLAG_UTTAK("GRUNNLAG_UTTAK", "Etabler grunnlag for uttak", UTREDES),
    KONTROLLER_FAKTA_UTTAK("KOFAKUT", "Kontroller fakta for uttak", UTREDES),
    KONTROLLER_AKTIVITETSKRAV("KONTROLLER_AKTIVITETSKRAV", "Kontroller aktivitetskrav", UTREDES),
    FAKTA_UTTAK("FAKTA_UTTAK", "Kontroller fakta for uttak", UTREDES),
    FAKTA_UTTAK_DOKUMENTASJON("FAKTA_UTTAK_DOKUMENTASJON", "Kontroller uttak dokumentasjon", UTREDES),

    // Kun for Svangerskapspenger
    VURDER_TILRETTELEGGING("VURDER_TLRG", "Vurder tilrettelegging for svangerskapspenger", UTREDES),
    VURDER_SVANGERSKAPSPENGERVILKÅR("VURDERSPV", "Vurder svangerskapspengervilkåret", UTREDES),

    // Engangsstønad og Foreldrepenger
    VARSEL_REVURDERING("VRSLREV", "Varsel om revurdering", UTREDES),
    SØKERS_RELASJON_TIL_BARN("VURDERSRB", "Vurder søkers relasjon til barnet", UTREDES),

    // Foreldrepenger og Svangerskapspenger
    KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING("KO_ARB_IM", "Kontroller arbeidsforhold", UTREDES),
    VURDER_ARB_FORHOLD_PERMISJON("VURDER_ARB_FORHOLD_PERMISJON", "Vurder arbeidsforhold med permisjon", UTREDES),
    FASTSETT_OPPTJENINGSPERIODE("VURDER_OPPTJ_PERIODE", "Vurder Opptjening Periode", UTREDES),
    VURDER_OPPTJENING_FAKTA("VURDER_OPPTJ_FAKTA", "Vurder opptjeningfakta", UTREDES),
    VURDER_OPPTJENINGSVILKÅR("VURDER_OPPTJ", "Vurder opptjeningsvilkåret", UTREDES),
    FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING("FASTSETT_STP_BER", "Fastsett skjæringstidspunkt beregning", UTREDES),
    KONTROLLER_FAKTA_BEREGNING("KOFAKBER", "Kontroller fakta for beregning", UTREDES),
    // Foreslå beregningsgrunnlag del 1, foreslår grunnlag for arbeid, frilans, arbeidsavklaringspenger, dagpenger, ytelse. Gjør avviksvurdering av arbeid / frilans
    FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN", "Foreslå beregningsgrunnlag", UTREDES),
    // Foreslå beregningsgrunnlag del 2, foreslår grunnlag for næring og militær og gjør avviksvurdering av næring
    FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN_2", "Fortsett foreslå beregningsgrunnlag", UTREDES),
    VURDER_VILKAR_BERGRUNN("VURDER_VILKAR_BERGRUNN", "Vurder beregingsgrunnlagsvilkåret", UTREDES),
    VURDER_REF_BERGRUNN("VURDER_REF_BERGRUNN", "Vurder refusjon for beregningsgrunnlaget", UTREDES),
    FORDEL_BEREGNINGSGRUNNLAG("FORDEL_BERGRUNN", "Fordel beregningsgrunnlag", UTREDES),
    FASTSETT_BEREGNINGSGRUNNLAG("FAST_BERGRUNN", "Fastsett beregningsgrunnlag", UTREDES),
    INNGANG_UTTAK("KOFAK_LOP_MEDL", "Inngangssteg for uttak", UTREDES), // Historisk kode-streng
    SØKNADSFRIST_FORELDREPENGER("SØKNADSFRIST_FP", "Vurder søknadsfrist foreldrepenger", UTREDES),
    VURDER_UTTAK("VURDER_UTTAK", "Vurder uttaksvilkår", UTREDES),
    VURDER_TILBAKETREKK("VURDER_TILBAKETREKK", "Vurder tilbaketrekk", UTREDES),
    HINDRE_TILBAKETREKK("BERYT_OPPDRAG", "Hindre tilbaketrekk", UTREDES),

    @Deprecated(forRemoval=true) // Gammelt steg som håndterte aksjonspunkt 5080 (erstattet av 5085 og steg KO_ARB_IM)
    KONTROLLER_FAKTA_ARBEIDSFORHOLD("KOARB", "Kontroller arbeidsforhold", UTREDES),
    @Deprecated(forRemoval=true) // Gammelt steg som kun logget oppførsel
    UTLED_FORUTGÅENDE_MEDLEMSKAPVILKÅR("VURDER_FORUTGÅENDE_MEDLEMSKAPVILKÅR", "Utled hvilket medlemskapsvilkår", UTREDES)
    ;

    static final String KODEVERK = "BEHANDLING_STEG_TYPE";

    private static final Map<String, BehandlingStegType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    /**
     * Definisjon av hvilken status behandlingen skal rapporteres som når dette steget er aktivt.
     */
    private BehandlingStatus definertBehandlingStatus;

    private String navn;

    private String kode;


    BehandlingStegType(String kode, String navn, BehandlingStatus definertBehandlingStatus) {
        this.kode = kode;
        this.navn = navn;
        this.definertBehandlingStatus = definertBehandlingStatus;
    }

    public BehandlingStatus getDefinertBehandlingStatus() {
        return definertBehandlingStatus;
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerInngang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, VurderingspunktType.INN);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjonerUtgang() {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, VurderingspunktType.UT);
    }

    public List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner(VurderingspunktType type) {
        return AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(this, type);
    }

    @Override
    @JsonValue // Swagger foreslår og tillater nå bare verdiene i feltet 'kode' som input. Fungere ikke når den er satt på field!
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    // Denne metoden brukes ifm @QueryParam ved setting av verdi fra kode. For at dette skal være mulig må navnet enten være 'fromString' eller 'valueOf'. 'valueOf' kan ikke overskrives.
    public static BehandlingStegType fromString(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStegType: " + kode);
        }
        return ad;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static Map<String, BehandlingStegType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingStegType, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingStegType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingStegType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : BehandlingStegType.fromString(dbData);
        }
    }


}
