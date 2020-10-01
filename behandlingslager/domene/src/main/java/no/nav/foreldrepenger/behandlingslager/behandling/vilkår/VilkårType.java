package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
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

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum VilkårType implements Kodeverdi {

    FØDSELSVILKÅRET_MOR(VilkårTypeKoder.FP_VK_1,
        "Fødselsvilkår Mor",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 1. ledd", FagsakYtelseType.FORELDREPENGER, "§ 14-5, 1. ledd"),
        Avslagsårsak.SØKT_FOR_TIDLIG,
        Avslagsårsak.SØKER_ER_MEDMOR,
        Avslagsårsak.SØKER_ER_FAR,
        Avslagsårsak.FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT,
        Avslagsårsak.ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR),
    FØDSELSVILKÅRET_FAR_MEDMOR(VilkårTypeKoder.FP_VK_11,
        "Fødselsvilkår for far/medmor",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-5, 1. ledd"),
        Avslagsårsak.INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR,
        Avslagsårsak.MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM,
        Avslagsårsak.BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET),
    ADOPSJONSVILKARET_FORELDREPENGER(VilkårTypeKoder.FP_VK_16,
        "Adopsjonsvilkåret Foreldrepenger",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-5, første ledd eller tredje ledd"),
        Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN),
    MEDLEMSKAPSVILKÅRET(VilkårTypeKoder.FP_VK_2,
        "Medlemskapsvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-2", FagsakYtelseType.FORELDREPENGER, "§ 14-2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    MEDLEMSKAPSVILKÅRET_LØPENDE(VilkårTypeKoder.FP_VK_2_L,
        "Løpende medlemskapsvilkår",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-2", FagsakYtelseType.FORELDREPENGER, "§ 14-2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    SØKNADSFRISTVILKÅRET(VilkårTypeKoder.FP_VK_3,
        "Søknadsfristvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 22-13, 2. ledd"),
        Avslagsårsak.SØKT_FOR_SENT),
    ADOPSJONSVILKÅRET_ENGANGSSTØNAD(VilkårTypeKoder.FP_VK_4,
        "Adopsjonsvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 1. ledd"),
        Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.EKTEFELLES_SAMBOERS_BARN,
        Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE,
        Avslagsårsak.ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    OMSORGSVILKÅRET(VilkårTypeKoder.FP_VK_5,
        "Omsorgsvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 3. ledd"),
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O,
        Avslagsårsak.MOR_IKKE_DØD,
        Avslagsårsak.MOR_IKKE_DØD_VED_FØDSEL_OMSORG,
        Avslagsårsak.ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.FAR_HAR_IKKE_OMSORG_FOR_BARNET,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    FORELDREANSVARSVILKÅRET_2_LEDD(VilkårTypeKoder.FP_VK_8,
        "Foreldreansvarsvilkåret 2.ledd",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 2. ledd", FagsakYtelseType.FORELDREPENGER, "§ 14-5, 2. ledd"),
        Avslagsårsak.BARN_IKKE_UNDER_15_ÅR,
        Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR,
        Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    FORELDREANSVARSVILKÅRET_4_LEDD(VilkårTypeKoder.FP_VK_33,
        "Foreldreansvarsvilkåret 4.ledd",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 4. ledd"),
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F,
        Avslagsårsak.OMSORGSOVERTAKELSE_ETTER_56_UKER,
        Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    SØKERSOPPLYSNINGSPLIKT(VilkårTypeKoder.FP_VK_34,
        "Søkers opplysningsplikt",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§§ 21-3", FagsakYtelseType.FORELDREPENGER, "§§ 21-3"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    OPPTJENINGSPERIODEVILKÅR(VilkårTypeKoder.FP_VK_21,
        "Opptjeningsperiode",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-6 og 14-10"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING),
    OPPTJENINGSVILKÅRET(VilkårTypeKoder.FP_VK_23,
        "Opptjeningsvilkåret",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-6"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING),
    BEREGNINGSGRUNNLAGVILKÅR(VilkårTypeKoder.FP_VK_41,
        "Beregning",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-7"),
        Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG),
    SVANGERSKAPSPENGERVILKÅR(VilkårTypeKoder.SVP_VK_1,
        "Svangerskapspengervilkåret",
        Map.of(FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-4"),
        Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE,
        Avslagsårsak.SØKER_ER_IKKE_I_ARBEID,
        Avslagsårsak.SØKER_SKULLE_IKKE_SØKT_SVP,
        Avslagsårsak.ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER,
        Avslagsårsak.ARBEIDSTAKER_KAN_OMPLASSERES,
        Avslagsårsak.SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER,
        Avslagsårsak.SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE),

    /**
     * Brukes i stedet for null der det er optional.
     */
    UDEFINERT("-", "Ikke definert", Map.of()),

    ;

    private static final Map<String, VilkårType> KODER = new LinkedHashMap<>();
    private static final Map<VilkårType, Set<Avslagsårsak>> INDEKS_VILKÅR_AVSLAGSÅRSAKER = new LinkedHashMap<>(); // NOSONAR
    private static final Map<Avslagsårsak, Set<VilkårType>> INDEKS_AVSLAGSÅRSAK_VILKÅR = new LinkedHashMap<>(); // NOSONAR
    public static final String KODEVERK = "VILKAR_TYPE";

    @JsonIgnore
    private Map<FagsakYtelseType, String> lovReferanser = Map.of();

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private Set<Avslagsårsak> avslagsårsaker;

    private String kode;

    private VilkårType(String kode) {
        this.kode = kode;
    }

    private VilkårType(String kode,
                      String navn,
                      Map<FagsakYtelseType, String> lovReferanser,
                      Avslagsårsak... avslagsårsaker) {
        this.kode = kode;
        this.navn = navn;
        this.lovReferanser = lovReferanser;
        this.avslagsårsaker = Set.of(avslagsårsaker);

    }

    public String getLovReferanse(FagsakYtelseType fagsakYtelseType) {
        return lovReferanser.get(fagsakYtelseType);
    }

    @Override
    public String toString() {
        return super.toString() + lovReferanser;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VilkårType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(VilkårType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VilkårType: " + kode);
        }
        return ad;
    }

    public static Map<String, VilkårType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public Set<Avslagsårsak> getAvslagsårsaker() {
        return avslagsårsaker;
    }

    public static Map<VilkårType, Set<Avslagsårsak>> finnAvslagårsakerGruppertPåVilkårType() {
        return Collections.unmodifiableMap(INDEKS_VILKÅR_AVSLAGSÅRSAKER);
    }

    public static Set<VilkårType> getVilkårTyper(Avslagsårsak avslagsårsak) {
        return INDEKS_AVSLAGSÅRSAK_VILKÅR.get(avslagsårsak);
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

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }

            INDEKS_VILKÅR_AVSLAGSÅRSAKER.put(v, v.avslagsårsaker);
            v.avslagsårsaker.forEach(a -> INDEKS_AVSLAGSÅRSAK_VILKÅR.computeIfAbsent(a, (k) -> new HashSet<>(4)).add(v));
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<VilkårType, String> {
        @Override
        public String convertToDatabaseColumn(VilkårType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public VilkårType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
