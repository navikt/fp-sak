package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.ES;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.FP;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.SVP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum KlageHjemmel implements Kodeverdi {

    MEDLEM("14-02", "14-2 Medlemskap", "14", "2", Set.of(ES, FP, SVP)),
    YSVP("14-04", "14-4 Rett på svangerskapspenger", "14", "4", Set.of(SVP)),
    YFP("14-05", "14-5 Rett på foreldrepenger", "14", "5", Set.of(FP)),
    OPPTJENING("14-06", "14-6 Opptjening", "14", "6", Set.of(FP, SVP)),
    BEREGNING("14-07", "14-7 Beregning", "14", "7", Set.of(FP, SVP)),
    DAGER("14-09", "14-9 Stønadsperioden", "14", "9", Set.of(FP)),
    UTTAK("14-10", "14-10 Uttaksperiodene", "14", "10", Set.of(FP)),
    UTSETTELSE("14-11", "14-11 Utsettelse", "14", "11", Set.of(FP)),
    KVOTER("14-12", "14-12 Uttak av kvoter", "14", "12", Set.of(FP)),
    AKTIVITET("14-13", "14-13 Fars uttak", "14", "13", Set.of(FP)),
    BFHR("14-14", "14-14 Bare far har rett", "14", "14", Set.of(FP)),
    FARALENE("14-15", "14-15 Far aleneomsorg", "14", "15", Set.of(FP)),
    GRADERING("14-16", "14-16 Gradert uttak", "14", "16", Set.of(FP)),
    YES("14-17", "14-17 Rett på engangsstønad", "14", "17", Set.of(ES)),
    OPPTJENINGSTID("8-2", "8-2 Opptjeningstid", "8", "2", Set.of(SVP)),
    OPPLYSNINGSPLIKT("21-3", "21-3 Opplysningsplikt", "21", "3", Set.of(ES, FP, SVP)),
    FREMSETT("22-13", "22-13 Fremsetning av krav", "22", "13", Set.of(ES, FP, SVP)),
    TILBAKE("22-15", "22-15 Tilbakekreving", "22", "15", Set.of(ES, FP, SVP)),
    EØS("883-6", "EØS 883/2004 artikkel 6", "EØS forordning 883/2004", "6", "", Set.of(FP, SVP)),

    UDEFINERT("-", "Ikke definert", null, null, Set.of()),
    ;

    private static final Map<String, KlageHjemmel> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_HJEMMEL";

    public static final String FOLKETRYGDLOVEN = "FOLKETRYGDLOVEN";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private String lov;
    @JsonIgnore
    private String kapittel;
    @JsonIgnore
    private String paragraf;
    @JsonIgnore
    private Set<FagsakYtelseType.YtelseType> ytelser;

    private KlageHjemmel(String kode) {
        this.kode = kode;
    }

    private KlageHjemmel(String kode, String navn, String kapittel, String paragraf, Set<FagsakYtelseType.YtelseType> ytelser) {
        this(kode, navn, FOLKETRYGDLOVEN, kapittel, paragraf, ytelser);
    }

    private KlageHjemmel(String kode, String navn, String lov, String kapittel, String paragraf, Set<FagsakYtelseType.YtelseType> ytelser) {
        this.kode = kode;
        this.navn = navn;
        this.lov = lov;
        this.kapittel = kapittel;
        this.paragraf = paragraf;
        this.ytelser = ytelser;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static KlageHjemmel fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        var kode = TempAvledeKode.getVerdi(KlageHjemmel.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageHjemmel: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageHjemmel> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KlageHjemmel, String> {
        @Override
        public String convertToDatabaseColumn(KlageHjemmel attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageHjemmel convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static List<KlageHjemmel> getHjemlerForYtelse(FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType == null) {
            return List.of();
        } else if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
            return new ArrayList<>(KODER.values());
        } else {
            var ytelse = FagsakYtelseType.YtelseType.valueOf(fagsakYtelseType.getKode());
            return KODER.values().stream().filter(h -> h.ytelser.contains(ytelse)).collect(Collectors.toList());
        }
    }

}
