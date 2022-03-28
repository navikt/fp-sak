package no.nav.foreldrepenger.behandlingslager.behandling;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus.UTREDES;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum BehandlingStegType implements Kodeverdi {

    // Steg koder som deles av alle ytelser
    VARSEL_REVURDERING(BehandlingStegKoder.VARSEL_REVURDERING_KODE, "Varsel om revurdering", UTREDES),
    INNHENT_SØKNADOPP(BehandlingStegKoder.INNHENT_SØKNADOPP_KODE, "Innhent søknadsopplysninger", UTREDES),
    INNHENT_REGISTEROPP(BehandlingStegKoder.INNHENT_REGISTEROPP_KODE, "Innhent registeropplysninger - innledende oppgaver", UTREDES),
    KONTROLLER_FAKTA(BehandlingStegKoder.KONTROLLER_FAKTA_KODE, "Kontroller Fakta", UTREDES),
    SØKERS_RELASJON_TIL_BARN(BehandlingStegKoder.SØKERS_RELASJON_TIL_BARN_KODE, "Vurder søkers relasjon til barnet", UTREDES),
    VURDER_MEDLEMSKAPVILKÅR(BehandlingStegKoder.VURDER_MEDLEMSKAPVILKÅR_KODE, "Vurder medlemskapvilkår", UTREDES),
    BEREGN_YTELSE(BehandlingStegKoder.BEREGN_YTELSE_KODE, "Beregn ytelse", UTREDES),
    FATTE_VEDTAK(BehandlingStegKoder.FATTE_VEDTAK_KODE, "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    IVERKSETT_VEDTAK(BehandlingStegKoder.IVERKSETT_VEDTAK_KODE, "Iverksett Vedtak", IVERKSETTER_VEDTAK),
    FORESLÅ_BEHANDLINGSRESULTAT(BehandlingStegKoder.FORESLÅ_BEHANDLINGSRESULTAT_KODE, "Foreslå behandlingsresultat", UTREDES),
    SIMULER_OPPDRAG(BehandlingStegKoder.SIMULER_OPPDRAG_KODE, "Simuler oppdrag", UTREDES),
    FORESLÅ_VEDTAK(BehandlingStegKoder.FORESLÅ_VEDTAK_KODE, "Foreslå vedtak", UTREDES),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT(BehandlingStegKoder.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT_KODE, "Kontrollerer søkers opplysningsplikt", UTREDES),
    KLAGE_VURDER_FORMKRAV_NFP(BehandlingStegKoder.KLAGE_VURDER_FORMKRAV_NFP_KODE, "Vurder formkrav (NFP)", UTREDES),
    KLAGE_NFP(BehandlingStegKoder.KLAGE_NFP_KODE, "Vurder Klage (NFP)", UTREDES),
    KLAGE_VURDER_FORMKRAV_NK(BehandlingStegKoder.KLAGE_VURDER_FORMKRAV_NK_KODE, "Vurder formkrav (NK)", UTREDES),
    KLAGE_NK(BehandlingStegKoder.KLAGE_NK_KODE, "Vurder Klage (NK)", UTREDES),
    ANKE(BehandlingStegKoder.ANKE_KODE, "Vurder anke", UTREDES),
    ANKE_MERKNADER(BehandlingStegKoder.ANKE_MERKNADER_KODE, "Vurder merknader", UTREDES),
    REGISTRER_SØKNAD(BehandlingStegKoder.REGISTRER_SØKNAD_KODE, "Registrer søknad", UTREDES),
    VURDER_INNSYN(BehandlingStegKoder.VURDER_INNSYN_KODE, "Vurder innsynskrav", UTREDES),
    INNHENT_PERSONOPPLYSNINGER(BehandlingStegKoder.INNHENT_PERSONOPPLYSNINGER_KODE, "Innhent personopplysninger", UTREDES),
    VURDER_KOMPLETTHET(BehandlingStegKoder.VURDER_KOMPLETTHET_KODE, "Vurder kompletthet", UTREDES),
    VURDER_SAMLET(BehandlingStegKoder.VURDER_SAMLET_KODE, "Vurder vilkår samlet", UTREDES),
    VURDER_TILBAKETREKK(BehandlingStegKoder.VURDER_TILBAKETREKK_KODE, "Vurder tilbaketrekk", UTREDES),
    VURDER_FARESIGNALER(BehandlingStegKoder.VURDER_FARESIGNALER_KODE, "Vurder faresignaler", UTREDES),
    VURDER_ARB_FORHOLD_PERMISJON(BehandlingStegKoder.VURDER_ARB_FORHOLD_PERMISJON_KODE, "Vurder arbeidsforhold med permisjon", UTREDES),

    // Kun for Foreldrepenger
    VURDER_UTTAK(BehandlingStegKoder.VURDER_UTTAK_KODE, "Vurder uttaksvilkår", UTREDES),
    VURDER_OPPTJENINGSVILKÅR(BehandlingStegKoder.VURDER_OPPTJENINGSVILKÅR_KODE, "Vurder opptjeningsvilkåret", UTREDES),
    FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING(BehandlingStegKoder.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING_KODE,
        "Fastsett skjæringstidspunkt beregning", UTREDES),
    KONTROLLER_FAKTA_BEREGNING(BehandlingStegKoder.KONTROLLER_FAKTA_BEREGNING_KODE, "Kontroller fakta for beregning", UTREDES),
    FORESLÅ_BESTEBEREGNING(BehandlingStegKoder.FORESLÅ_BESTEBEREGNING_KODE, "Foreslå besteberegning", UTREDES),
    FORESLÅ_BEREGNINGSGRUNNLAG(BehandlingStegKoder.FORESLÅ_BEREGNINGSGRUNNLAG_KODE, "Foreslå beregningsgrunnlag", UTREDES),
    VURDER_VILKAR_BERGRUNN(BehandlingStegKoder.VURDER_VILKAR_BERGRUNN_KODE, "Vurder beregingsgrunnlagsvilkåret", UTREDES),
    FASTSETT_BEREGNINGSGRUNNLAG(BehandlingStegKoder.FASTSETT_BEREGNINGSGRUNNLAG_KODE, "Fastsett beregningsgrunnlag", UTREDES),
    SØKNADSFRIST_FORELDREPENGER(BehandlingStegKoder.SØKNADSFRIST_FORELDREPENGER_KODE, "Vurder søknadsfrist foreldrepenger", UTREDES),
    KONTROLLER_FAKTA_UTTAK(BehandlingStegKoder.KONTROLLER_FAKTA_UTTAK_KODE, "Kontroller fakta for uttak", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD(BehandlingStegKoder.KONTROLLER_FAKTA_ARBEIDSFORHOLD_KODE, "Kontroller arbeidsforhold", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING(BehandlingStegKoder.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING_KODE, "Kontroller arbeidsforhold", UTREDES),
    FASTSETT_OPPTJENINGSPERIODE(BehandlingStegKoder.FASTSETT_OPPTJENINGSPERIODE_KODE, "Vurder Opptjening Periode", UTREDES),
    KONTROLLER_LØPENDE_MEDLEMSKAP(BehandlingStegKoder.KONTROLLER_LØPENDE_MEDLEMSKAP_KODE, "Kontroller løpende medlemskap", UTREDES),
    HINDRE_TILBAKETREKK(BehandlingStegKoder.HINDRE_TILBAKETREKK_KODE, "Hindre tilbaketrekk", UTREDES),



    // Kun for Engangsstønad
    VURDER_SØKNADSFRISTVILKÅR(BehandlingStegKoder.VURDER_SØKNADSFRISTVILKÅR_KODE, "Vurder felles inngangsvilkår", UTREDES),

    // Kun for Svangerskapspenger
    VURDER_TILRETTELEGGING(BehandlingStegKoder.VURDER_TILRETTELEGGING_KODE, "Vurder tilrettelegging for svangerskapspenger", UTREDES),
    VURDER_SVANGERSKAPSPENGERVILKÅR(BehandlingStegKoder.VURDER_SVANGERSKAPSPENGERVILKÅR_KODE, "Vurder svangerskapspengervilkåret", UTREDES),

    // Steg koder som deles av Foreldrepenger og Svangerskapspenger
    FORDEL_BEREGNINGSGRUNNLAG(BehandlingStegKoder.FORDEL_BEREGNINGSGRUNNLAG_KODE, "Fordel beregningsgrunnlag", UTREDES),
    VURDER_REF_BERGRUNN(BehandlingStegKoder.VURDER_REF_BERGRUNN_KODE, "Vurder refusjon for beregningsgrunnlaget", UTREDES),
    VULOMED(BehandlingStegKoder.VULOMED_KODE, "Vurder løpende medlemskap", UTREDES),
    INREG_AVSL(BehandlingStegKoder.INREG_AVSL_KODE, "Innhent registeropplysninger - resterende oppgaver", UTREDES),
    VURDER_OPPTJENING_FAKTA(BehandlingStegKoder.VURDER_OPPTJENING_FAKTA_KODE, "Vurder opptjeningfakta", UTREDES),
    KONTROLLER_AKTIVITETSKRAV(BehandlingStegKoder.KONTROLLER_AKTIVITETSKRAV_KODE, "Kontroller aktivitetskrav", UTREDES);


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

    @JsonValue
    private String kode;

    BehandlingStegType(String kode) {
        this.kode = kode;
    }

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

    public static BehandlingStegType fraKode(String kode) {
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
            return dbData == null ? null : BehandlingStegType.fraKode(dbData);
        }
    }


}
