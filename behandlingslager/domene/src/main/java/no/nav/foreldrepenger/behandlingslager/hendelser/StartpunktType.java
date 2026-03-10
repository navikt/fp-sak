package no.nav.foreldrepenger.behandlingslager.hendelser;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum StartpunktType implements Kodeverdi, DatabaseKode {

    KONTROLLER_ARBEIDSFORHOLD("KONTROLLER_ARBEIDSFORHOLD", "Startpunkt kontroller arbeidsforhold", 1, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING, Set.of(FagsakYtelseType.ENGANGSTØNAD)),
    KONTROLLER_FAKTA("KONTROLLER_FAKTA", "Kontroller fakta", 2, BehandlingStegType.KONTROLLER_FAKTA, Set.of()),
    INNGANGSVILKÅR_OPPLYSNINGSPLIKT("INNGANGSVILKÅR_OPPL", "Inngangsvilkår opplysningsplikt", 3, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, Set.of()),
    SØKERS_RELASJON_TIL_BARNET("SØKERS_RELASJON_TIL_BARNET", "Søkers relasjon til barnet", 4, BehandlingStegType.SØKERS_RELASJON_TIL_BARN, Set.of(FagsakYtelseType.SVANGERSKAPSPENGER)),
    INNGANGSVILKÅR_MEDLEMSKAP("INNGANGSVILKÅR_MEDL", "Inngangsvilkår medlemskapsvilkår", 5, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, Set.of()),
    OPPTJENING("OPPTJENING", "Opptjening", 6, BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE, Set.of(FagsakYtelseType.ENGANGSTØNAD)),
    DEKNINGSGRAD("DEKNINGSGRAD", "Dekningsgrad", 7, BehandlingStegType.DEKNINGSGRAD, Set.of(FagsakYtelseType.ENGANGSTØNAD, FagsakYtelseType.SVANGERSKAPSPENGER)),
    BEREGNING("BEREGNING", "Beregning", 8, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, Set.of(FagsakYtelseType.ENGANGSTØNAD)),
    // StartpunktType BEREGNING_FORESLÅ skal kun brukes ved G-regulering
    BEREGNING_FORESLÅ("BEREGNING_FORESLÅ", "Beregning foreslå", 9, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, Set.of(FagsakYtelseType.ENGANGSTØNAD)),
    UTTAKSVILKÅR("UTTAKSVILKÅR", "Uttaksvilkår", 10, BehandlingStegType.INNGANG_UTTAK, Set.of(FagsakYtelseType.ENGANGSTØNAD)), // OBS: Endrer du startsteg må du flytte køhåndtering ....
    TILKJENT_YTELSE("TILKJENT_YTELSE", "Tilkjent ytelse", 11, BehandlingStegType.BEREGN_YTELSE, Set.of()), // OBS: Ikke testet for Engangsstønad

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", 99, BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, Set.of()),
    ;

    private static final Map<String, StartpunktType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    static final Map<StartpunktType, Set<VilkårType>> VILKÅR_HÅNDTERT_INNEN_STARTPUNKT = new EnumMap<>(StartpunktType.class);
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
            Set.of(VilkårType.SØKERSOPPLYSNINGSPLIKT));

        // Medlemskap
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP)
            .addAll(Set.of(VilkårType.FØDSELSVILKÅRET_MOR, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, VilkårType.OMSORGSOVERTAKELSEVILKÅR,
                VilkårType.SVANGERSKAPSPENGERVILKÅR));

        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.OPPTJENING,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.OPPTJENING).addAll(Set.of(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE));

        // Dekningsgrad
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.DEKNINGSGRAD,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.DEKNINGSGRAD)
            .addAll(Set.of(VilkårType.OPPTJENINGSPERIODEVILKÅR, VilkårType.OPPTJENINGSVILKÅRET));

        // Beregning
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.BEREGNING,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));

        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.BEREGNING_FORESLÅ,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));

        // Uttak
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.UTTAKSVILKÅR,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.UTTAKSVILKÅR).add(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        // Tilkjent
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.put(StartpunktType.TILKJENT_YTELSE,
            VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.values().stream().flatMap(Collection::stream).collect(toSet()));
        VILKÅR_HÅNDTERT_INNEN_STARTPUNKT.get(StartpunktType.TILKJENT_YTELSE).add(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE);
    }

    private final BehandlingStegType behandlingSteg;

    private final int rangering;

    private final String navn;

    private final Set<FagsakYtelseType> unntakYtelseTyper;

    @JsonValue
    private final String kode;

    StartpunktType(String kode, String navn, int rangering, BehandlingStegType stegType, Set<FagsakYtelseType> unntakYtelseTyper) {
        this.kode = kode;
        this.navn = navn;
        this.rangering = rangering;
        this.behandlingSteg = stegType;
        this.unntakYtelseTyper = unntakYtelseTyper;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return this.kode;
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

    public int getRangering() {
        return rangering;
    }

    public static Set<StartpunktType> startpunktForYtelse(FagsakYtelseType ytelseType) {
        return Arrays.stream(StartpunktType.values()).filter(s -> !s.unntakYtelseTyper.contains(ytelseType)).collect(toSet());
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

        private static StartpunktType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent StartpunktType: " + kode);
            }
            return ad;
        }
    }
}
