package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

public enum FeltNavnType {
    ADOPSJONSVILKARET("ADOPSJONSVILKARET", "Adopsjonsvilkåret"),
    OPPTJENINGSVILKARET("OPPTJENINGSVILKARET", "Opptjeningsvilkåret"),
    MEDLEMSKAPSVILKÅRET("MEDLEMSKAPSVILKÅRET", "Medlemskap"),
    MEDLEMSKAPSVILKÅRET_OPPHØRSDATO("MEDLEMSKAPSVILKÅRET_OPPHØRSDATO", "Opphørt medlemskap"),
    MEDLEMSKAPSVILKÅRET_MEDLEMFRADATO("MEDLEMSKAPSVILKÅRET_MEDLEMFRADATO", "Innflyttingsdato"),
    ADOPTERER_ALENE("ADOPTERER_ALENE", "Adopterer alene"),
    AKTIVITET("AKTIVITET", "Aktivitet {value}"),
    AKTIVITET_PERIODE("AKTIVITET_PERIODE", "Perioden med aktivitet {value} er"),
    ALENEOMSORG("ALENEOMSORG", "Aleneomsorg"),
    ANTALL_BARN("ANTALL_BARN", "Antall barn"),
    AVKLARSAKSOPPLYSNINGER("AVKLARSAKSOPPLYSNINGER", "Personstatus"),
    BEHANDLENDE_ENHET("BEHANDLENDE_ENHET", "Behandlende enhet"),
    BEHANDLING("BEHANDLING", "Behandling"),
    BRUK_ANTALL_I_SOKNAD("BRUK_ANTALL_I_SOKNAD", "Bruk antall fra søknad"),
    BRUK_ANTALL_I_TPS("BRUK_ANTALL_I_TPS", "Bruk antall fra folkeregisteret"),
    BRUK_ANTALL_I_VEDTAKET("BRUK_ANTALL_I_VEDTAKET", "Bruk antall fra vedtaket"),
    BRUTTO_NAERINGSINNTEKT("BRUTTO_NAERINGSINNTEKT", "Brutto næringsinntekt"),
    DODSDATO("DODSDATO", "Dødsdato"),
    DOKUMENTASJON_FORELIGGER("DOKUMENTASJON_FORELIGGER", "Dokumentasjon foreligger"),
    EKTEFELLES_BARN("EKTEFELLES_BARN", "Ektefelles/samboers barn"),
    ENDRING_NAERING("ENDRING_NAERING", "Endring i næringsvirksomhet"),
    ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD("ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD", "Arbeidsforhold hos {value}"),
    ER_SOKER_BOSATT_I_NORGE("ER_SOKER_BOSATT_I_NORGE", "Bosted"),
    FODSELSVILKARET("FODSELSVILKARET", "Fødselsvilkåret"),
    FODSELSDATO("FODSELSDATO", "Fødselsdato"),
    FORDELING_FOR_ANDEL("FORDELING_FOR_ANDEL", "{value}"),
    FORDELING_FOR_NY_ANDEL("FORDELING_FOR_NY_ANDEL", "{value}"),
    FORDELING_ETTER_BESTEBEREGNING("FORDELING_ETTER_BESTEBEREGNING", "Fordeling etter besteberegning"),
    FORELDREANSVARSVILKARET("FORELDREANSVARSVILKARET", "Foreldreansvarsvilkåret"),
    FRILANS_INNTEKT("FRILANS_INNTEKT", "Frilansinntekt"),
    FRILANSVIRKSOMHET("FRILANSVIRKSOMHET", "Frilansvirksomhet"),
    GYLDIG_MEDLEM_FOLKETRYGDEN("GYLDIG_MEDLEM_FOLKETRYGDEN", "Vurder om søker har gyldig medlemskap i perioden"),
    INNTEKT_FRA_ARBEIDSFORHOLD("INNTEKT_FRA_ARBEIDSFORHOLD", "Inntekt fra {value}"),
    LØNNSENDRING_I_PERIODEN("LØNNSENDRING_I_PERIODEN", "Lønnsendring siste tre måneder"),
    MANN_ADOPTERER("MANN_ADOPTERER", "Mann adopterer"),
    MOTTAR_YTELSE_ARBEID("MOTTAR_YTELSE_ARBEID", "Mottar søker ytelse for arbeid i {value}"),
    MOTTAR_YTELSE_FRILANS("MOTTAR_YTELSE_FRILANS", "Mottar søker ytelse for frilansaktiviteten"),
    MOTTATT_DATO("MOTTATT_DATO", "Dato for når søknaden kan anses som mottatt"),
    OMSORG("OMSORG", "Omsorg"),
    OMSORGSOVERTAKELSESDATO("OMSORGSOVERTAKELSESDATO", "Omsorgsovertakelsesdato"),
    OMSORGSVILKAR("OMSORGSVILKAR", "Omsorgsvilkåret"),
    IKKE_OMSORG_PERIODEN("IKKE_OMSORG_PERIODEN", "Søker har ikke omsorg for barnet i perioden"),
    INNTEKTSKATEGORI_FOR_ANDEL("INNTEKTSKATEGORI_FOR_ANDEL", "Inntektskategori for {value}"),
    OPPHOLDSRETT_EOS("OPPHOLDSRETT_EOS", "Oppholdsrett"),
    OPPHOLDSRETT_IKKE_EOS("OPPHOLDSRETT_IKKE_EOS", "Lovlig opphold"),
    OVERSTYRT_BEREGNING("OVERSTYRT_BEREGNING", "Overstyrt beregning:; Beløpet er endret fra"),
    OVERSTYRT_VURDERING("OVERSTYRT_VURDERING", "Overstyrt vurdering:; Utfallet er endret fra"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    SOKERSOPPLYSNINGSPLIKT("SOKERSOPPLYSNINGSPLIKT", "Søkers opplysningsplikt"),
    SVANGERSKAPSPENGERVILKÅRET("SVANGERSKAPSPENGERVILKÅRET", "Svangerskapsvilkåret"),
    SOKNADSFRIST("SOKNADSFRIST", "Søknadsfrist"),
    SOKNADSFRISTVILKARET("SOKNADSFRISTVILKARET", "Søknadsfristvilkåret"),
    STARTDATO_FRA_SOKNAD("STARTDATO_FRA_SOKNAD", "Startdato fra søknad"),
    TERMINBEKREFTELSE("TERMINBEKREFTELSE", "Terminbekreftelse"),
    TERMINDATO("TERMINDATO", "Termindato"),
    UTSTEDTDATO("UTSTEDTDATO", "Utstedt dato"),
    VILKAR_SOM_ANVENDES("VILKAR_SOM_ANVENDES", "Vilkår som anvendes"),
    FASTSETT_RESULTAT_PERIODEN("FASTSETT_RESULTAT_PERIODEN", "Resultat for perioden"),
    AVKLART_PERIODE("AVKLART_PERIODE", "Avklart periode"),
    ANDEL_ARBEID("ANDEL_ARBEID", "Andel i arbeid"),
    UTTAK_TREKKDAGER("UTTAK_TREKKDAGER", "Trekk"),
    UTTAK_STØNADSKONTOTYPE("UTTAK_STØNADSKONTOTYPE", "Stønadskonto"),
    UTTAK_PERIODE_RESULTAT_TYPE("UTTAK_PERIODE_RESULTAT_TYPE", "Resultatet"),
    UTTAK_PROSENT_UTBETALING("UTTAK_PROSENT_UTBETALING", "Utbetalingsgrad"),
    UTTAK_SAMTIDIG_UTTAK("UTTAK_SAMTIDIG_UTTAK", "Samtidig uttak"),
    UTTAK_TREKKDAGER_FLERBARN_KVOTE("UTTAK_TREKKDAGER_FLERBARN_KVOTE", "Flerbarnsdager"),
    UTTAK_PERIODE_RESULTAT_ÅRSAK("UTTAK_PERIODE_RESULTAT_ÅRSAK", "Årsak resultat"),
    UTTAK_GRADERING_ARBEIDSFORHOLD("UTTAK_GRADERING_ARBEIDSFORHOLD", "Gradering av arbeidsforhold"),
    UTTAK_GRADERING_AVSLAG_ÅRSAK("UTTAK_GRADERING_AVSLAG_ÅRSAK", "Årsak avslag gradering"),
    UTTAK_SPLITT_TIDSPERIODE("UTTAK_SPLITT_TIDSPERIODE", "UTTAK_SPLITT_TIDSPERIODE"), //FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    SYKDOM("SYKDOM", "Sykdom"),
    ARBEIDSFORHOLD("ARBEIDSFORHOLD", "Arbeidsforhold hos {value}"),
    NY_FORDELING("NY_FORDELING", "Ny fordeling <b>{value}</b>"),
    NY_AKTIVITET("NY_AKTIVITET", "Det er lagt til ny aktivitet for <b>{value}</b>"), //TODO Thao: Test denne og sjekk om {value} blir håndtert. Generelt sjekk de koder som har {value} i seg.
    NYTT_REFUSJONSKRAV("NYTT_REFUSJONSKRAV", "Nytt refusjonskrav"),
    INNTEKTSKATEGORI("INNTEKTSKATEGORI", "Inntektskategori"),
    FNR("FNR", "Fødselsnummer"),
    PERIODE_FOM("PERIODE_FOM", "Periode f.o.m."),
    PERIODE_TOM("PERIODE_TOM", "Periode t.o.m."),
    MANDAT("MANDAT", "Mandat"),
    KONTAKTPERSON("KONTAKTPERSON", "Kontaktperson"),
    BRUKER_TVUNGEN("BRUKER_TVUNGEN", "Søker er under tvungen forvaltning"),
    TYPE_VERGE("TYPE_VERGE", "Type verge"),
    DAGPENGER_INNTEKT("DAGPENGER_INNTEKT", "Dagpenger"),
    KLAGE_RESULTAT_NFP("KLAGE_RESULTAT_NFP", "Resultat"),
    KLAGE_RESULTAT_KA("KLAGE_RESULTAT_KA", "Ytelsesvedtak"),
    KLAGE_OMGJØR_ÅRSAK("KLAGE_OMGJØR_ÅRSAK", "Årsak til omgjøring"),
    ER_KLAGER_PART("ER_KLAGER_PART", "Er klager part i saken"),
    ER_KLAGE_KONKRET("ER_KLAGE_KONKRET", "Klages det på konkrete elementer i vedtaket"),
    ER_KLAGEFRIST_OVERHOLDT("ER_KLAGEFRIST_OVERHOLDT", "Er klagefristen overholdt"),
    ER_KLAGEN_SIGNERT("ER_KLAGEN_SIGNERT", "Er klagen signert"),
    PA_KLAGD_BEHANDLINGID("PA_KLAGD_BEHANDLINGID", "Vedtaket som er påklagd"),
    ANKE_RESULTAT("ANKE_RESULTAT", "Vedtaket som er anket"),
    KONTROLL_AV_BESTEBEREGNING("KONTROLL_AV_BESTEBEREGNING", "Godkjenning av automatisk besteberegning"),
    ANKE_OMGJØR_ÅRSAK("ANKE_OMGJØR_ÅRSAK", "Omgjøringsårsak"),
    ER_ANKER_IKKE_PART("ER_ANKER_IKKE_PART", "Er anker ikke part"),
    ER_ANKE_IKKE_KONKRET("ER_ANKE_IKKE_KONKRET", "Er anke ikke konkret"),
    ER_ANKEFRIST_IKKE_OVERHOLDT("ER_ANKEFRIST_IKKE_OVERHOLDT", "Oppfylt"),
    ER_ANKEN_IKKE_SIGNERT("ER_ANKEN_IKKE_SIGNERT", "Er anken ikke signer"),
    PA_ANKET_BEHANDLINGID("PA_ANKET_BEHANDLINGID", "Vedtaket som er påklagd"),
    VURDER_ETTERLØNN_SLUTTPAKKE("VURDER_ETTERLØNN_SLUTTPAKKE", "Inntekt fra etterlønn eller sluttpakke"),
    FASTSETT_ETTERLØNN_SLUTTPAKKE("FASTSETT_ETTERLØNN_SLUTTPAKKE", "Inntekten"),
    ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT("ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT", "ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT"), //FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON("ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON", "ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON"), //FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    FASTSETT_VIDERE_BEHANDLING("FASTSETT_VIDERE_BEHANDLING", "FASTSETT_VIDERE_BEHANDLING"), //FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    RETT_TIL_FORELDREPENGER("RETT_TIL_FORELDREPENGER", "Rett til foreldrepenger"),
    MOR_MOTTAR_UFØRETRYGD("MOR_MOTTAR_UFØRETRYGD", "Mor mottar uføretrygd"),
    MOR_MOTTAR_STØNAD_EØS("MOR_MOTTAR_STØNAD_EØS", "MOR_MOTTAR_STØNAD_EØS"), //FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    ANNEN_FORELDER_RETT_EØS("ANNEN_FORELDER_RETT_EØS", "Annen forelder har tilstrekkelig opptjening fra land i EØS"),
    VURDER_GRADERING_PÅ_ANDEL_UTEN_BG("VURDER_GRADERING_PÅ_ANDEL_UTEN_BG", "Inntektsgrunnlag ved gradering"),
    DEKNINGSGRAD("DEKNINGSGRAD", "Dekningsgrad"),
    TILBAKETREKK("TILBAKETREKK", "Tilbaketrekk"),
    SAKSMARKERING("SAKSMARKERING", "Saksmarkering"),
    INNHENT_SED("INNHENT_SED", "Innhent dokumentasjon"),
    HEL_TILRETTELEGGING_FOM("HEL_TILRETTELEGGING_FOM", "Hel tilrettelegging fra og med"),
    DELVIS_TILRETTELEGGING_FOM("DELVIS_TILRETTELEGGING_FOM", "Delvis tilrettelegging fra og med"),
    STILLINGSPROSENT("STILLINGSPROSENT", "stillingsprosent"),
    SLUTTE_ARBEID_FOM("SLUTTE_ARBEID_FOM", "Slutte arbeid fra og med"),
    TILRETTELEGGING_BEHOV_FOM("TILRETTELEGGING_BEHOV_FOM", "Tilrettelegging er nødvendig fra og med"),
    TILRETTELEGGING_SKAL_BRUKES("TILRETTELEGGING_SKAL_BRUKES", "Tilrettelegging skal brukes"),
    FARESIGNALER("FARESIGNALER", "Resultat"),
    MILITÆR_ELLER_SIVIL("MILITÆR_ELLER_SIVIL", "Har søker militær- eller siviltjeneste i opptjeningsperioden"),
    NY_REFUSJONSFRIST("NY_REFUSJONSFRIST", "Utvidelse av frist for fremsatt refusjonskrav for {value}"),
    NY_STARTDATO_REFUSJON("NY_STARTDATO_REFUSJON", "Startdato for refusjon til {value}"),
    DELVIS_REFUSJON_FØR_STARTDATO("DELVIS_REFUSJON_FØR_STARTDATO", "Delvis refusjon før "),
    ORGANISASJONSNUMMER("ORGANISASJONSNUMMER", "Organisasjonsnummer"),
    ARBEIDSFORHOLD_BEKREFTET_TOM_DATO("ARBEIDSFORHOLD_BEKREFTET_TOM_DATO", "ARBEIDSFORHOLD_BEKREFTET_TOM_DATO"), //FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    ANKE_AVVIST_ÅRSAK("ANKE_AVVIST_ÅRSAK", "Avvisningsårsak"),
    AKTIVITETSKRAV_AVKLARING("AKTIVITETSKRAV_AVKLARING", "AKTIVITETSKRAV_AVKLARING"),//FIXME Thao: Ikke i frontend. Sjekk hva som skal være her!
    UTTAKPERIODE_DOK_AVKLARING("UTTAKPERIODE_DOK_AVKLARING", "Perioden {value}"),
    FAKTA_UTTAK_PERIODE("FAKTA_UTTAK_PERIODE", "Perioden {value}"),
    SVP_OPPHOLD_PERIODE("SVP_OPPHOLD_PERIODE", "Periode med opphold"),
    VURDERT_ETTERBETALING_TIL_SØKER("VURDERT_ETTERBETALING_TIL_SØKER", "Vurdering av etterbetaling til søker");


    private final Object key;
    private final String text;

    FeltNavnType(Object key, String text) {
        this.key = key;
        this.text = text;
    }

    public Object getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public static FeltNavnType getByKey(Object key) {
        for (FeltNavnType feltNavnType : values()) {
            if (feltNavnType.getKey().equals(key)) {
                return feltNavnType;
            }
        }
        throw new IllegalStateException("Finner ikke FeltNavnType konstant for nøkkel=" + key);
    }


}
