package no.nav.foreldrepenger.datavarehus.xml.fp;

import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;


enum UttakDokumentasjonType {

    UTEN_OMSORG("UTEN_OMSORG", "Søker har ikke omsorg for barnet"),
    ALENEOMSORG("ALENEOMSORG", "Søker har aleneomsorg for barnet"),
    ANNEN_FORELDER_HAR_RETT("ANNEN_FORELDER_HAR_RETT", "Annenforelder har rett for barnet"),
    ANNEN_FORELDER_RETT_EOS("ANNEN_FORELDER_RETT_EOS", "Annenforelder har opptjening fra EØS"),
    SYK_SØKER("SYK_SOKER", "Søker er syk eller skadet"),
    INNLAGT_SØKER("INNLAGT_SOKER", "Søker er innlagt i institusjon"),
    INNLAGT_BARN("INNLAGT_BARN", "Barn er innlagt i institusjon"),
    INSTITUSJONSOPPHOLD_ANNEN_FORELDRE("INSTITUSJONSOPPHOLD_ANNEN_FORELDRE", "Annen forelder er innlagt i institusjon"),
    SYKDOM_ANNEN_FORELDER("SYKDOM_ANNEN_FORELDER", "Annen forelder er syk eller skadet"),
    IKKE_RETT_ANNEN_FORELDER("IKKE_RETT_ANNEN_FORELDER", "Annen forelder har ikke rett for barnet"),
    ALENEOMSORG_OVERFØRING("ALENEOMSORG_OVERFØRING", "Annen forelder har ikke omsorg for barnet"),
    HV_OVELSE("HV_OVELSE", "Søker er i tjeneste eller øvelse i heimevernet"),
    NAV_TILTAK("NAV_TILTAK", "Søker er i tiltak i regi av Nav"),
    MOR_AKTIVITET("MOR_AKTIVITET", "Mor er i aktivitet"),
    ;

    private final String navn;
    private final String kode;

    UttakDokumentasjonType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    KodeverksOpplysning tilKodeverksOpplysning() {
        var kodeverksOpplysning = VedtakXmlUtil.lagTomKodeverksOpplysning();
        kodeverksOpplysning.setKode(kode);
        kodeverksOpplysning.setValue(navn);
        kodeverksOpplysning.setKodeverk("UTTAK_DOKUMENTASJON_TYPE");
        return kodeverksOpplysning;
    }
}
