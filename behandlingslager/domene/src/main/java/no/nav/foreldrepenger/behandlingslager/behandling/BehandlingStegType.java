package no.nav.foreldrepenger.behandlingslager.behandling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus.IVERKSETTER_VEDTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus.UTREDES;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingStegType implements Kodeverdi {

    // Steg koder som deles av alle ytelser
    VARSEL_REVURDERING("VRSLREV", "Varsel om revurdering", UTREDES),
    INNHENT_SØKNADOPP("INSØK", "Innhent søknadsopplysninger", UTREDES),
    INNHENT_REGISTEROPP("INREG", "Innhent registeropplysninger - innledende oppgaver", UTREDES),
    KONTROLLER_FAKTA("KOFAK", "Kontroller Fakta", UTREDES),
    SØKERS_RELASJON_TIL_BARN("VURDERSRB", "Vurder søkers relasjon til barnet", UTREDES),
    VURDER_MEDLEMSKAPVILKÅR("VURDERMV", "Vurder medlemskapvilkår", UTREDES),
    BEREGN_YTELSE("BERYT", "Beregn ytelse", UTREDES),
    FATTE_VEDTAK("FVEDSTEG", "Fatte Vedtak", BehandlingStatus.FATTER_VEDTAK),
    IVERKSETT_VEDTAK("IVEDSTEG", "Iverksett Vedtak", IVERKSETTER_VEDTAK),
    FORESLÅ_BEHANDLINGSRESULTAT("FORBRES", "Foreslå behandlingsresultat", UTREDES),
    SIMULER_OPPDRAG("SIMOPP", "Simuler oppdrag", UTREDES),
    FORESLÅ_VEDTAK("FORVEDSTEG", "Foreslå vedtak", UTREDES),
    KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT("VURDEROP", "Kontrollerer søkers opplysningsplikt", UTREDES),
    KLAGE_VURDER_FORMKRAV_NFP("VURDER_FK_UI", "Vurder formkrav (NFP)", UTREDES),
    KLAGE_NFP("KLAGEUI", "Vurder Klage (NFP)", UTREDES),
    KLAGE_VURDER_FORMKRAV_NK("VURDER_FK_OI", "Vurder formkrav (NK)", UTREDES),
    KLAGE_NK("KLAGEOI", "Vurder Klage (NK)", UTREDES),
    ANKE("ANKE", "Vurder anke", UTREDES),
    ANKE_MERKNADER("ANKE_MERKNADER", "Vurder merknader", UTREDES),
    REGISTRER_SØKNAD("REGSØK", "Registrer søknad", UTREDES),
    VURDER_INNSYN("VURDINNSYN", "Vurder innsynskrav", UTREDES),
    INNHENT_PERSONOPPLYSNINGER("INPER", "Innhent personopplysninger", UTREDES),
    VURDER_KOMPLETTHET("VURDERKOMPLETT", "Vurder kompletthet", UTREDES),
    VURDER_SAMLET("VURDERSAMLET", "Vurder vilkår samlet", UTREDES),
    VURDER_TILBAKETREKK("VURDER_TILBAKETREKK", "Vurder tilbaketrekk", UTREDES),
    VURDER_FARESIGNALER("VURDER_FARESIGNALER", "Vurder faresignaler", UTREDES),
    VURDER_ARB_FORHOLD_PERMISJON("VURDER_ARB_FORHOLD_PERMISJON", "Vurder arbeidsforhold med permisjon", UTREDES),

    // Kun for Foreldrepenger
    VURDER_UTTAK("VURDER_UTTAK", "Vurder uttaksvilkår", UTREDES),
    VURDER_OPPTJENINGSVILKÅR("VURDER_OPPTJ", "Vurder opptjeningsvilkåret", UTREDES),
    FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING("FASTSETT_STP_BER",
        "Fastsett skjæringstidspunkt beregning", UTREDES),
    KONTROLLER_FAKTA_BEREGNING("KOFAKBER", "Kontroller fakta for beregning", UTREDES),
    FORESLÅ_BESTEBEREGNING("FORS_BESTEBEREGNING", "Foreslå besteberegning", UTREDES),
    FORESLÅ_BEREGNINGSGRUNNLAG("FORS_BERGRUNN", "Foreslå beregningsgrunnlag", UTREDES),
    VURDER_VILKAR_BERGRUNN("VURDER_VILKAR_BERGRUNN", "Vurder beregingsgrunnlagsvilkåret", UTREDES),
    FASTSETT_BEREGNINGSGRUNNLAG("FAST_BERGRUNN", "Fastsett beregningsgrunnlag", UTREDES),
    SØKNADSFRIST_FORELDREPENGER("SØKNADSFRIST_FP", "Vurder søknadsfrist foreldrepenger", UTREDES),
    KONTROLLER_FAKTA_UTTAK("KOFAKUT", "Kontroller fakta for uttak", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD("KOARB", "Kontroller arbeidsforhold", UTREDES),
    KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING("KO_ARB_IM", "Kontroller arbeidsforhold", UTREDES),
    FASTSETT_OPPTJENINGSPERIODE("VURDER_OPPTJ_PERIODE", "Vurder Opptjening Periode", UTREDES),
    KONTROLLER_LØPENDE_MEDLEMSKAP("KOFAK_LOP_MEDL", "Kontroller løpende medlemskap", UTREDES),
    HINDRE_TILBAKETREKK("BERYT_OPPDRAG", "Hindre tilbaketrekk", UTREDES),



    // Kun for Engangsstønad
    VURDER_SØKNADSFRISTVILKÅR("VURDERSFV", "Vurder felles inngangsvilkår", UTREDES),

    // Kun for Svangerskapspenger
    VURDER_TILRETTELEGGING("VURDER_TLRG", "Vurder tilrettelegging for svangerskapspenger", UTREDES),
    VURDER_SVANGERSKAPSPENGERVILKÅR("VURDERSPV", "Vurder svangerskapspengervilkåret", UTREDES),

    // Steg koder som deles av Foreldrepenger og Svangerskapspenger
    FORDEL_BEREGNINGSGRUNNLAG("FORDEL_BERGRUNN", "Fordel beregningsgrunnlag", UTREDES),
    VURDER_REF_BERGRUNN("VURDER_REF_BERGRUNN", "Vurder refusjon for beregningsgrunnlaget", UTREDES),
    VULOMED("VULOMED", "Vurder løpende medlemskap", UTREDES),
    INREG_AVSL("INREG_AVSL", "Innhent registeropplysninger - resterende oppgaver", UTREDES),
    VURDER_OPPTJENING_FAKTA("VURDER_OPPTJ_FAKTA", "Vurder opptjeningfakta", UTREDES),
    KONTROLLER_AKTIVITETSKRAV("KONTROLLER_AKTIVITETSKRAV", "Kontroller aktivitetskrav", UTREDES);


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
    @JsonIgnore
    private BehandlingStatus definertBehandlingStatus;

    @JsonIgnore
    private String navn;

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

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonCreator
    public static BehandlingStegType fraKode(@JsonProperty("kode") String kode) {
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
