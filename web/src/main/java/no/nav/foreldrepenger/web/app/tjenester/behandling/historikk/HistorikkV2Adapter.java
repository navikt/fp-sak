package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import jakarta.ws.rs.core.UriBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkAvklartSoeknadsperiodeType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagDokumentLinkDto;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HistorikkV2Adapter {

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID) {
        return switch (h.getType()) {
            case BEH_GJEN,
                 KØET_BEH_GJEN,
                 BEH_MAN_GJEN,
                 BEH_STARTET,
                 BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbike?
                 VEDLEGG_MOTTATT,
                 BREV_SENT, BREV_BESTILT,
                 MIN_SIDE_ARBEIDSGIVER,
                 REVURD_OPPR,
                 REGISTRER_PAPIRSØK,
                 MANGELFULL_SØKNAD,
                 INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT,
                 NYE_REGOPPLYSNINGER,
                 BEH_AVBRUTT_VUR,
                 BEH_OPPDATERT_NYE_OPPL,
                 SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR, fptilbake
                 MIGRERT_FRA_INFOTRYGD,
                 MIGRERT_FRA_INFOTRYGD_FJERNET,
                 ANKEBEH_STARTET,
                 KLAGEBEH_STARTET,
                 ENDRET_DEKNINGSGRAD,
                 OPPGAVE_VEDTAK -> fraMaltype1(h, behandlingUUID);
            case FORSLAG_VEDTAK,
                 FORSLAG_VEDTAK_UTEN_TOTRINN,
                 VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que?
                 UENDRET_UTFALL,
                 REGISTRER_OM_VERGE -> fraMaltype2(h, behandlingUUID);
            case SAK_GODKJENT,
                 FAKTA_ENDRET,
                 KLAGE_BEH_NK,
                 KLAGE_BEH_NFP,
                 BYTT_ENHET,
                 UTTAK,
                 TERMINBEKREFTELSE_UGYLDIG,
                 ANKE_BEH -> fraMalType5(h, behandlingUUID);

        };
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .toList();

        var hendelseTekst = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getHendelse().stream())
            .map(HistorikkinnslagFelt::getNavn)
            .map(HistorikkinnslagType::fraKode)
            .map(HistorikkinnslagType::getNavn)
            .toList();
        var resultatTekst = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getResultat().stream())
            .map(HistorikkV2Adapter::fraHistorikkResultat)
            .toList();

        // {lagGjeldendeFraInnslag(historikkinnslagDel)}
        var gjeldendeFraInnslag = h.getHistorikkinnslagDeler().stream()
            .flatMap(del -> del.getGjeldendeFraFelt().stream()
                .map(felt -> tilGjeldendeFraInnslag(felt, del)))
            .toList();

        // soeknadsperiode
        var søknadsperiode = h.getHistorikkinnslagDeler().stream()
                .flatMap(del -> del.getAvklartSoeknadsperiode().stream())
                .map(HistorikkV2Adapter::fraSøknadsperiode)
                .toList();

        var tema = h.getHistorikkinnslagDeler().stream()
                .flatMap(del -> del.getTema().stream())
                .map(HistorikkV2Adapter::fraTema)
                .toList();

        var endretFelter = h.getHistorikkinnslagDeler().stream()
                .flatMap(del -> del.getEndredeFelt().stream())
                .map(felt -> fraEndretFelt(felt))
                .toList();

        return null;
    }

    private static String fraEndretFelt(HistorikkinnslagFelt felt) {
        var endretFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());

        var tekstNavn = switch (endretFeltNavn) {
            case ADOPSJONSVILKARET -> "Adopsjonsvilkåret";
            case OPPTJENINGSVILKARET -> "Opptjeningsvilkåret";
            case MEDLEMSKAPSVILKÅRET -> "Medlemskap";
            case MEDLEMSKAPSVILKÅRET_OPPHØRSDATO -> "Opphørt medlemskap";
            case MEDLEMSKAPSVILKÅRET_MEDLEMFRADATO -> "Innflyttingsdato";
            case ADOPTERER_ALENE -> "Adopterer alene";
            case AKTIVITET -> "Aktivitet {value}";
            case AKTIVITET_PERIODE -> "Perioden med aktivitet {value} er";
            case ALENEOMSORG -> "Aleneomsorg";
            case ANTALL_BARN -> "Antall barn";
            case AVKLARSAKSOPPLYSNINGER -> "Personstatus";
            case BEHANDLENDE_ENHET -> "Behandlende enhet";
            case BEHANDLING -> "Behandling";
            case BRUK_ANTALL_I_SOKNAD -> "Bruk antall fra søknad";
            case BRUK_ANTALL_I_TPS -> "Bruk antall fra folkeregisteret";
            case BRUK_ANTALL_I_VEDTAKET -> "Bruk antall fra vedtaket";
            case BRUTTO_NAERINGSINNTEKT -> "Brutto næringsinntekt";
            case DODSDATO -> "Dødsdato";
            case DOKUMENTASJON_FORELIGGER -> "Dokumentasjon foreligger";
            case EKTEFELLES_BARN -> "";
            case ENDRING_NAERING -> "";
            case ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD -> "";
            case ER_SOKER_BOSATT_I_NORGE -> "";
            case FODSELSVILKARET -> "";
            case FODSELSDATO -> "";
            case FORDELING_FOR_ANDEL -> "";
            case FORDELING_FOR_NY_ANDEL -> "";
            case FORDELING_ETTER_BESTEBEREGNING -> "";
            case FORELDREANSVARSVILKARET -> "";
            case FRILANS_INNTEKT -> "";
            case FRILANSVIRKSOMHET -> "";
            case GYLDIG_MEDLEM_FOLKETRYGDEN -> "";
            case INNTEKT_FRA_ARBEIDSFORHOLD -> "";
            case LØNNSENDRING_I_PERIODEN -> "";
            case MANN_ADOPTERER -> "";
            case MOTTAR_YTELSE_ARBEID -> "";
            case MOTTAR_YTELSE_FRILANS -> "";
            case MOTTATT_DATO -> "";
            case OMSORG -> "";
            case OMSORGSOVERTAKELSESDATO -> "";
            case OMSORGSVILKAR -> "";
            case IKKE_OMSORG_PERIODEN -> "";
            case INNTEKTSKATEGORI_FOR_ANDEL -> "";
            case OPPHOLDSRETT_EOS -> "";
            case OPPHOLDSRETT_IKKE_EOS -> "";
            case OVERSTYRT_BEREGNING -> "";
            case OVERSTYRT_VURDERING -> "";
            case SELVSTENDIG_NÆRINGSDRIVENDE -> "";
            case SOKERSOPPLYSNINGSPLIKT -> "";
            case SVANGERSKAPSPENGERVILKÅRET -> "";
            case SOKNADSFRIST -> "";
            case SOKNADSFRISTVILKARET -> "";
            case STARTDATO_FRA_SOKNAD -> "";
            case TERMINBEKREFTELSE -> "";
            case TERMINDATO -> "";
            case UTSTEDTDATO -> "";
            case VILKAR_SOM_ANVENDES -> "";
            case FASTSETT_RESULTAT_PERIODEN -> "";
            case AVKLART_PERIODE -> "";
            case ANDEL_ARBEID -> "";
            case UTTAK_TREKKDAGER -> "";
            case UTTAK_STØNADSKONTOTYPE -> "";
            case UTTAK_PERIODE_RESULTAT_TYPE -> "";
            case UTTAK_PROSENT_UTBETALING -> "";
            case UTTAK_SAMTIDIG_UTTAK -> "";
            case UTTAK_TREKKDAGER_FLERBARN_KVOTE -> "";
            case UTTAK_PERIODE_RESULTAT_ÅRSAK -> "";
            case UTTAK_GRADERING_ARBEIDSFORHOLD -> "";
            case UTTAK_GRADERING_AVSLAG_ÅRSAK -> "";
            case UTTAK_SPLITT_TIDSPERIODE -> "";
            case SYKDOM -> "";
            case ARBEIDSFORHOLD -> "";
            case NY_FORDELING -> "";
            case NY_AKTIVITET -> "";
            case NYTT_REFUSJONSKRAV -> "";
            case INNTEKT -> "";
            case INNTEKTSKATEGORI -> "";
            case NAVN -> "";
            case FNR -> "";
            case PERIODE_FOM -> "";
            case PERIODE_TOM -> "";
            case MANDAT -> "";
            case KONTAKTPERSON -> "";
            case BRUKER_TVUNGEN -> "";
            case TYPE_VERGE -> "";
            case DAGPENGER_INNTEKT -> "";
            case KLAGE_RESULTAT_NFP -> "";
            case KLAGE_RESULTAT_KA -> "";
            case KLAGE_OMGJØR_ÅRSAK -> "";
            case ER_KLAGER_PART -> "";
            case ER_KLAGE_KONKRET -> "";
            case ER_KLAGEFRIST_OVERHOLDT -> "";
            case ER_KLAGEN_SIGNERT -> "";
            case PA_KLAGD_BEHANDLINGID -> "";
            case ANKE_RESULTAT -> "";
            case KONTROLL_AV_BESTEBEREGNING -> "";
            case ANKE_OMGJØR_ÅRSAK -> "";
            case ER_ANKER_IKKE_PART -> "";
            case ER_ANKE_IKKE_KONKRET -> "";
            case ER_ANKEFRIST_IKKE_OVERHOLDT -> "";
            case ER_ANKEN_IKKE_SIGNERT -> "";
            case PA_ANKET_BEHANDLINGID -> "";
            case VURDER_ETTERLØNN_SLUTTPAKKE -> "";
            case FASTSETT_ETTERLØNN_SLUTTPAKKE -> "";
            case ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT -> "";
            case ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON -> "";
            case FASTSETT_VIDERE_BEHANDLING -> "";
            case RETT_TIL_FORELDREPENGER -> "";
            case MOR_MOTTAR_UFØRETRYGD -> "";
            case MOR_MOTTAR_STØNAD_EØS -> "";
            case ANNEN_FORELDER_RETT_EØS -> "";
            case VURDER_GRADERING_PÅ_ANDEL_UTEN_BG -> "";
            case DEKNINGSGRAD -> "";
            case TILBAKETREKK -> "";
            case SAKSMARKERING -> "";
            case INNHENT_SED -> "";
            case HEL_TILRETTELEGGING_FOM -> "";
            case DELVIS_TILRETTELEGGING_FOM -> "";
            case STILLINGSPROSENT -> "";
            case SLUTTE_ARBEID_FOM -> "";
            case TILRETTELEGGING_BEHOV_FOM -> "";
            case TILRETTELEGGING_SKAL_BRUKES -> "";
            case FARESIGNALER -> "";
            case MILITÆR_ELLER_SIVIL -> "";
            case NY_REFUSJONSFRIST -> "";
            case NY_STARTDATO_REFUSJON -> "";
            case DELVIS_REFUSJON_FØR_STARTDATO -> "";
            case ORGANISASJONSNUMMER -> "";
            case ARBEIDSFORHOLD_BEKREFTET_TOM_DATO -> "";
            case ANKE_AVVIST_ÅRSAK -> "";
            case AKTIVITETSKRAV_AVKLARING -> "";
            case UTTAKPERIODE_DOK_AVKLARING -> "";
            case FAKTA_UTTAK_PERIODE -> "";
            case SVP_OPPHOLD_PERIODE -> "";
            case VURDERT_ETTERBETALING_TIL_SØKER -> "";
            case UDEFINIERT -> throw new IllegalStateException("UDEFINTER ");
        };


        var endretFeltVerdiFra= null;
        var endretFeltVerdiTil= null;

    }

    private static String findEndretFeltVerdi(HistorikkinnslagDel del, String verdi) {
        if (verdi == null) return "";
        if ("true".equals(verdi)) {
            return "Ja";
        }
        if ("false".equals(verdi)) {
            return "Nei";
        }
        var verdiTekstFraKode = HistorikkEndretFeltVerdiType.fraKode(verdi);


        return verdiTekstFraKode;
    }
    private static String convertBoolean(String verdi){
        if ("true".equals(verdi)) {
            return "Ja";
        }
        if ("false".equals(verdi)) {
            return "Nei";
        }
        return verdi;
    }
    private static String fraTema(HistorikkinnslagFelt tema) {
        var type = HistorikkEndretFeltType.fraKode(tema.getNavn());
        var tekst = switch (type) {
            case AKTIVITET -> "<b>Det er lagt til ny aktivitet:</b> <br/><b>{value}</b>"; // Finnes ikke frontend
            case FORDELING_FOR_NY_ANDEL -> "<b>Det er lagt til ny aktivitet:</b> <br/><b>{value}</b>: Fordeling for ";
            case FORDELING_FOR_ANDEL -> "Fordeling for <b>{value}</b>:";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        return String.format(tekst, tema.getNavnVerdi());
    }

    private static String fraSøknadsperiode(HistorikkinnslagFelt søknadsperiode) {
        var type = HistorikkAvklartSoeknadsperiodeType.fraKode(søknadsperiode.getNavn());

        var tekst =  switch (type){
            case GRADERING -> "<b>Uttak: gradering</b> <br/>%s<br/>%s";
            case UTSETTELSE_ARBEID -> "<b>Utsettelse: Arbeid</b> <br/>%s";
            case UTSETTELSE_FERIE -> "<b>Utsettelse: Ferie</b> <br/>%s";
            case UTSETTELSE_SKYDOM -> "<b>Utsettelse: Sykdom/skade</b> <br/>%s";
            case UTSETTELSE_HV -> "<b>Utsettelse: Heimevernet</b> <br/>%s";
            case UTSETTELSE_TILTAK_I_REGI_AV_NAV -> "<b>Utsettelse: Tiltak i regi av NAV</b> <br/>%s";
            case UTSETTELSE_INSTITUSJON_SØKER -> "<b>Utsettelse: Innleggelse av forelder</b> <br/>%s";
            case UTSETTELSE_INSTITUSJON_BARN -> "<b>Utsettelse: Innleggelse av barn</b> <br/>%s";
            case NY_SOEKNADSPERIODE -> "<b>Ny periode er lagt til</b> <br/>%s";
            case SLETTET_SOEKNASPERIODE -> "<b>Perioden er slettet</b> <br/>%s";
            case OVERFOERING_ALENEOMSORG -> "<b>Overføring: søker har aleneomsorg</b> <br/>%s";
            case OVERFOERING_SKYDOM -> "<b>Overføring: sykdom/skade</b> <br/>%s";
            case OVERFOERING_INNLEGGELSE -> "<b>Overføring: innleggelse</b> <br/>%s";
            case OVERFOERING_IKKE_RETT -> "<b>Overføring: annen forelder har ikke rett</b> <br/>%s";
            case UTTAK -> "<b>Uttak</b> <br/>%s";
            case OPPHOLD -> "<b>Opphold: annen foreldres uttak</b> <br/>%s";
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        return type.equals(HistorikkAvklartSoeknadsperiodeType.GRADERING)
                ? String.format(tekst, søknadsperiode.getNavnVerdi(), søknadsperiode.getTilVerdi())
                : String.format(tekst, søknadsperiode.getTilVerdi());
    }

    private static String tilGjeldendeFraInnslag(HistorikkinnslagFelt gjeldendeFra, HistorikkinnslagDel del) {
        // Fra DB: Hverken navn, navnverdi, tilVerdi er null (FPSAK)
        var historikkEndretFeltType = HistorikkEndretFeltType.fraKode(gjeldendeFra.getNavn());

        var endretFeltTekst = switch (historikkEndretFeltType) {
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for <b>%s</b>";
            case NY_FORDELING -> "Ny fordeling <b>%s</b>";
            default -> throw new IllegalArgumentException();
        };
        // Historikk.Template.5.VerdiGjeldendeFra
        var verditekst = String.format(" gjeldende fra <b>%s</b>:", gjeldendeFra.getTilVerdi());

        return String.format(endretFeltTekst, gjeldendeFra.getNavnVerdi()) + verditekst + (del.getEndredeFelt().isEmpty()
            ? "Ingen endring av vurdering"
            : "");
    }


    private static String fraHistorikkResultat(String type) {
        var historikkResultatType = HistorikkResultatType.valueOf(type);
        return switch (historikkResultatType) {
            case AVVIS_KLAGE -> "Klagen er avvist";
            case MEDHOLD_I_KLAGE -> "Vedtaket er omgjort";
            case OPPHEVE_VEDTAK -> "Vedtaket er opphevet";
            case OPPRETTHOLDT_VEDTAK -> "Vedtaket er opprettholdt";
            case STADFESTET_VEDTAK -> "Vedtaket er stadfestet";
            case BEREGNET_AARSINNTEKT -> "Grunnlag for beregnet årsinntekt";
            case UTFALL_UENDRET -> "Overstyrt vurdering: Utfall er uendret";
            case DELVIS_MEDHOLD_I_KLAGE -> "Vedtaket er delvis omgjort";
            case KLAGE_HJEMSENDE_UTEN_OPPHEVE -> "Behandling er hjemsendt";
            case UGUNST_MEDHOLD_I_KLAGE -> "Vedtaket er omgjort til ugunst";
            case OVERSTYRING_FAKTA_UTTAK -> "Overstyrt vurdering:";
            case ANKE_AVVIS,
                 ANKE_OMGJOER,
                 ANKE_OPPHEVE_OG_HJEMSENDE,
                 ANKE_HJEMSENDE,
                 ANKE_STADFESTET_VEDTAK,
                 ANKE_DELVIS_OMGJOERING_TIL_GUNST,
                 ANKE_TIL_UGUNST,
                 ANKE_TIL_GUNST -> historikkResultatType.getNavn(); // Ikke i frontend
            default -> historikkResultatType.getNavn();
        };
    }



    private static HistorikkinnslagDtoV2 fraMaltype2(Historikkinnslag h, UUID behandlingUUID) {

        return null; // TODO:
    }


    private static HistorikkinnslagDtoV2 fraMaltype1(Historikkinnslag innslag, UUID behandlingUUID) {
        var historikkinnslagDel = innslag.getHistorikkinnslagDeler().getFirst();
        var skjermlenke = historikkinnslagDel.getSkjermlenke().map(SkjermlenkeType::fraKode).orElse(null);
        var lenker = tilDto(innslag.getDokumentLinker());
        var begrunnelsetekst = historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .orElseGet(() -> historikkinnslagDel.getBegrunnelse().orElse(null));

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(innslag.getAktør(), innslag.getOpprettetAv()),
            skjermlenke,
            innslag.getOpprettetTidspunkt(),
            lenker,
            innslag.getType().getNavn(),
            begrunnelsetekst
        );
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDto(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        if (dokumentLinker == null) {
            return null;
        }
        return dokumentLinker.stream()
            .map(d -> tilDto(d, null, null)) // TODO: husks aktiv journalpost og full URI
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDto(HistorikkinnslagDokumentLink lenke, List<JournalpostId> journalPosterForSak, URI dokumentPath) {
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        dto.setUtgått(aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak));
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        if (lenke.getJournalpostId().getVerdi() != null && lenke.getDokumentId() != null) {
            var builder = UriBuilder.fromUri(dokumentPath)
                .queryParam("journalpostId", lenke.getJournalpostId().getVerdi())
                .queryParam("dokumentId", lenke.getDokumentId());
            dto.setUrl(builder.build());
        }
        return dto;
    }

    private static boolean aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst().isEmpty();
    }


    // Fra HistorikkinnslagDelTo
    private static Optional<Kodeverdi> finnÅrsakKodeListe(HistorikkinnslagFelt aarsak) {

        var aarsakVerdi = aarsak.getTilVerdi();
        if (Objects.equals("-", aarsakVerdi)) {
            return Optional.empty();
        }
        if (aarsak.getKlTilVerdi() == null) {
            return Optional.empty();
        }

        var kodeverdiMap = HistorikkInnslagTekstBuilder.KODEVERK_KODEVERDI_MAP.get(aarsak.getKlTilVerdi());
        if (kodeverdiMap == null) {
            throw new IllegalStateException("Har ikke støtte for HistorikkinnslagFelt#klTilVerdi=" + aarsak.getKlTilVerdi());
        }
        return Optional.ofNullable(kodeverdiMap.get(aarsakVerdi));
    }
}
