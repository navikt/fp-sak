package no.nav.foreldrepenger.behandlingslager.behandling.vilkår;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum VilkårType implements Kodeverdi {

    FØDSELSVILKÅRET_MOR("FP_VK_1",
        "Fødselsvilkår Mor",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 1. ledd", FagsakYtelseType.FORELDREPENGER, "§ 14-5, 1. ledd"),
        Avslagsårsak.SØKT_FOR_TIDLIG,
        Avslagsårsak.SØKER_ER_MEDMOR,
        Avslagsårsak.SØKER_ER_FAR,
        Avslagsårsak.FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT,
        Avslagsårsak.ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR),
    FØDSELSVILKÅRET_FAR_MEDMOR("FP_VK_11",
        "Fødselsvilkår for far/medmor",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-5, 1. ledd"),
        Avslagsårsak.INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR,
        Avslagsårsak.MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM,
        Avslagsårsak.BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET),
    OMSORGSOVERTAKELSEVILKÅR("FP_VK_6",
        "Adopsjon og foreldreansvar",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17", FagsakYtelseType.FORELDREPENGER, "§ 14-5"),
        Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.EKTEFELLES_SAMBOERS_BARN,
        Avslagsårsak.ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FAR_HAR_IKKE_OMSORG_FOR_BARNET,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA,
        Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE,
        Avslagsårsak.MOR_IKKE_DØD,
        Avslagsårsak.MOR_IKKE_DØD_VED_FØDSEL_OMSORG,
        Avslagsårsak.OMSORGSOVERTAKELSE_ETTER_56_UKER,
        Avslagsårsak.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN,
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F,
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O,
        Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET,
        Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR),
    ADOPSJONSVILKARET_FORELDREPENGER("FP_VK_16",
        "Adopsjonsvilkåret Foreldrepenger",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-5, første ledd eller tredje ledd"),
        Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN),
    MEDLEMSKAPSVILKÅRET("FP_VK_2",
        "Medlemskapsvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-2", FagsakYtelseType.FORELDREPENGER, "§ 14-2", FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    MEDLEMSKAPSVILKÅRET_FORUTGÅENDE("FP_VK_2_F",
        "Medlemskapsvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, femte ledd"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT,
        Avslagsårsak.SØKER_INNFLYTTET_FOR_SENT),
    MEDLEMSKAPSVILKÅRET_LØPENDE("FP_VK_2_L",
        "Løpende medlemskapsvilkår",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-2", FagsakYtelseType.FORELDREPENGER, "§ 14-2", FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-2"),
        Avslagsårsak.SØKER_ER_IKKE_MEDLEM,
        Avslagsårsak.SØKER_ER_UTVANDRET,
        Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT,
        Avslagsårsak.SØKER_ER_IKKE_BOSATT),
    SØKNADSFRISTVILKÅRET("FP_VK_3",
        "Søknadsfristvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 22-13, 2. ledd"),
        Avslagsårsak.SØKT_FOR_SENT),
    ADOPSJONSVILKÅRET_ENGANGSSTØNAD("FP_VK_4",
        "Adopsjonsvilkåret",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 1. ledd"),
        Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.EKTEFELLES_SAMBOERS_BARN,
        Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE,
        Avslagsårsak.ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    OMSORGSVILKÅRET("FP_VK_5",
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
    FORELDREANSVARSVILKÅRET_2_LEDD("FP_VK_8",
        "Foreldreansvarsvilkåret 2.ledd",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 2. ledd", FagsakYtelseType.FORELDREPENGER, "§ 14-5, 2. ledd"),
        Avslagsårsak.BARN_OVER_15_ÅR,
        Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR,
        Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    FORELDREANSVARSVILKÅRET_4_LEDD("FP_VK_33",
        "Foreldreansvarsvilkåret 4.ledd",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§ 14-17, 4. ledd"),
        Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F,
        Avslagsårsak.OMSORGSOVERTAKELSE_ETTER_56_UKER,
        Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA,
        Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
        Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR),
    SØKERSOPPLYSNINGSPLIKT("FP_VK_34",
        "Søkers opplysningsplikt",
        Map.of(FagsakYtelseType.ENGANGSTØNAD, "§§ 21-3", FagsakYtelseType.FORELDREPENGER, "§§ 21-3", FagsakYtelseType.SVANGERSKAPSPENGER, "§ 21-3"),
        Avslagsårsak.MANGLENDE_DOKUMENTASJON),
    OPPTJENINGSPERIODEVILKÅR("FP_VK_21",
        "Opptjeningsperiode",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-6 og 14-10", FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-4"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING),
    OPPTJENINGSVILKÅRET("FP_VK_23",
        "Opptjeningsvilkåret",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-6", FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-4"),
        Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING),
    BEREGNINGSGRUNNLAGVILKÅR("FP_VK_41",
        "Beregning",
        Map.of(FagsakYtelseType.FORELDREPENGER, "§ 14-7", FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-4"),
        Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG),
    SVANGERSKAPSPENGERVILKÅR("SVP_VK_1",
        "Svangerskapspengervilkåret",
        Map.of(FagsakYtelseType.SVANGERSKAPSPENGER, "§ 14-4"),
        Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE,
        Avslagsårsak.SØKER_ER_IKKE_I_ARBEID,
        Avslagsårsak.SØKER_HAR_MOTTATT_SYKEPENGER,
        Avslagsårsak.ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER,
        Avslagsårsak.ARBEIDSTAKER_KAN_OMPLASSERES,
        Avslagsårsak.SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER,
        Avslagsårsak.SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE),

    /**
     * Brukes i stedet for null der det er optional.
     */
    UDEFINERT("-", "Ikke definert", Map.of()),

    ;

    private static final Set<VilkårType> RELASJON_TIL_BARN = Set.of(VilkårType.FØDSELSVILKÅRET_MOR, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR,
        VilkårType.OMSORGSOVERTAKELSEVILKÅR,
        VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, VilkårType.ADOPSJONSVILKARET_FORELDREPENGER,
        VilkårType.OMSORGSVILKÅRET, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD);

    private static final Map<String, VilkårType> KODER = new LinkedHashMap<>();
    private static final Map<VilkårType, Set<Avslagsårsak>> INDEKS_VILKÅR_AVSLAGSÅRSAKER = new LinkedHashMap<>();
    private static final Map<Avslagsårsak, Set<VilkårType>> INDEKS_AVSLAGSÅRSAK_VILKÅR = new LinkedHashMap<>();
    public static final String KODEVERK = "VILKAR_TYPE";

    private Map<FagsakYtelseType, String> lovReferanser = Map.of();

    private String navn;

    private Set<Avslagsårsak> avslagsårsaker;

    @JsonValue
    private String kode;

    VilkårType(String kode,
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

    public boolean gjelderRelasjonTilBarn() {
        return RELASJON_TIL_BARN.contains(this);
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }

            INDEKS_VILKÅR_AVSLAGSÅRSAKER.put(v, v.avslagsårsaker);
            v.avslagsårsaker.forEach(a -> INDEKS_AVSLAGSÅRSAK_VILKÅR.computeIfAbsent(a, k -> new HashSet<>(4)).add(v));
        }
    }

    public boolean gjelderMedlemskap() {
        return this.equals(VilkårType.MEDLEMSKAPSVILKÅRET) || VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE.equals(this);
    }

    public boolean erInngangsvilkår() {
        return !Set.of(VilkårType.UDEFINERT, MEDLEMSKAPSVILKÅRET_LØPENDE).contains(this);
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

        private static VilkårType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent VilkårType: " + kode);
            }
            return ad;
        }
    }

}
