package no.nav.foreldrepenger.behandlingslager.hendelser;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum StartpunktType implements Kodeverdi {

    KONTROLLER_ARBEIDSFORHOLD("KONTROLLER_ARBEIDSFORHOLD", "Startpunkt kontroller arbeidsforhold", 1, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD),
    KONTROLLER_FAKTA("KONTROLLER_FAKTA", "Kontroller fakta", 2, BehandlingStegType.KONTROLLER_FAKTA),
    INNGANGSVILKÅR_OPPLYSNINGSPLIKT("INNGANGSVILKÅR_OPPL", "Inngangsvilkår opplysningsplikt", 3, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
    SØKERS_RELASJON_TIL_BARNET("SØKERS_RELASJON_TIL_BARNET", "Søkers relasjon til barnet", 4, BehandlingStegType.SØKERS_RELASJON_TIL_BARN),
    INNGANGSVILKÅR_MEDLEMSKAP("INNGANGSVILKÅR_MEDL", "Inngangsvilkår medlemskapsvilkår", 5, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR),
    OPPTJENING("OPPTJENING", "Opptjening", 6, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE),
    BEREGNING("BEREGNING", "Beregning", 7, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING),
    // StartpunktType BEREGNING_FORESLÅ skal kun brukes ved G-regulering
    BEREGNING_FORESLÅ("BEREGNING_FORESLÅ", "Beregning foreslå", 8, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG),
    UTTAKSVILKÅR("UTTAKSVILKÅR", "Uttaksvilkår", 9, BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP), // OBS: Endrer du startsteg må du flytte køhåndtering ....

    UDEFINERT("-", "Ikke definert", 99, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT),
    ;

    public static final String KODEVERK = "STARTPUNKT_TYPE";
    private static final Map<String, StartpunktType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    static Map<StartpunktType, Set<VilkårType>> VILKÅR_HÅNDTERT_INNEN_STARTPUNKT = new HashMap<>();
    static {
        // Kontroller arbeidsforhold - ingen vilkår håndter før dette startpunktet
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.KONTROLLER_ARBEIDSFORHOLD,
            new HashSet<>());

        // Kontroller Fakta - ingen vilkår håndter før dette startpunktet
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.KONTROLLER_FAKTA,
            new HashSet<>());

        // Opplysningsplikt - ingen vilkår håndter før dette startpunktet
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT,
            new HashSet<>());

        // Søkers relasjon
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.SØKERS_RELASJON_TIL_BARNET,
            new HashSet<>(Collections.singleton(VilkårType.SØKERSOPPLYSNINGSPLIKT)));

        // Medlemskap
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP).addAll(new HashSet<>(
            Arrays.asList(VilkårType.FØDSELSVILKÅRET_MOR, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
                VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårType.OMSORGSVILKÅRET, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD,
                VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD)));

        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.OPPTJENING,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.OPPTJENING).addAll(new HashSet<>(
            Collections.singletonList(VilkårType.MEDLEMSKAPSVILKÅRET)));

        // Beregning
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.BEREGNING,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.BEREGNING).addAll(new HashSet<>(
            Arrays.asList(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET)));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.BEREGNING_FORESLÅ,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));

        // Uttak
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.UTTAKSVILKÅR,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.UTTAKSVILKÅR).addAll(new HashSet<>(
            Collections.singletonList(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)));
    }

    @JsonIgnore
    private BehandlingStegType behandlingSteg;

    @JsonIgnore
    private int rangering;

    @JsonIgnore
    private String navn;

    private String kode;

    private StartpunktType(String kode) {
        this.kode = kode;
    }

    private StartpunktType(String kode, String navn, int rangering, BehandlingStegType stegType) {
        this.kode = kode;
        this.navn = navn;
        this.rangering = rangering;
        this.behandlingSteg = stegType;
    }

    @JsonCreator
    public static StartpunktType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent StartpunktType: " + kode);
        }
        return ad;
    }

    public static Map<String, StartpunktType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return this.kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingSteg;
    }

    public String getTransisjonIdentifikator() {
        return "revurdering-fremover-til-" + getBehandlingSteg().getKode();
    }

    public static Set<VilkårType> finnVilkårHåndtertInnenStartpunkt(StartpunktType startpunkt) {
        return VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(startpunkt);
    }

    public static Set<StartpunktType> inngangsVilkårStartpunkt() {
        return Set.of(SØKERS_RELASJON_TIL_BARNET, INNGANGSVILKÅR_OPPLYSNINGSPLIKT, INNGANGSVILKÅR_MEDLEMSKAP);
    }

    public int getRangering() {
        return rangering;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<StartpunktType, String> {
        @Override
        public String convertToDatabaseColumn(StartpunktType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public StartpunktType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
