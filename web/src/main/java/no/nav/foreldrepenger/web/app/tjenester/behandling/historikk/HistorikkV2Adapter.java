package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
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

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_FOM;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType.UTTAK_PERIODE_TOM;

public class HistorikkV2Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter.class);

    public static HistorikkinnslagDtoV2 map(Historikkinnslag h, UUID behandlingUUID) {
        return switch (h.getType()) {
            case BEH_GJEN, KØET_BEH_GJEN, BEH_MAN_GJEN, BEH_STARTET, BEH_STARTET_PÅ_NYTT,
                 // BEH_STARTET_FORFRA,  finnes ikke lenger? fptilbike?
                 VEDLEGG_MOTTATT, BREV_SENT, BREV_BESTILT, MIN_SIDE_ARBEIDSGIVER, REVURD_OPPR, REGISTRER_PAPIRSØK, MANGELFULL_SØKNAD, INNSYN_OPPR,
                 VRS_REV_IKKE_SNDT, NYE_REGOPPLYSNINGER, BEH_AVBRUTT_VUR, BEH_OPPDATERT_NYE_OPPL, SPOLT_TILBAKE,
                 // TILBAKEKREVING_OPPR, fptilbake
                 MIGRERT_FRA_INFOTRYGD, MIGRERT_FRA_INFOTRYGD_FJERNET, ANKEBEH_STARTET, KLAGEBEH_STARTET, ENDRET_DEKNINGSGRAD, OPPGAVE_VEDTAK ->
                fraMaltype1(h, behandlingUUID);
            case FORSLAG_VEDTAK, FORSLAG_VEDTAK_UTEN_TOTRINN, VEDTAK_FATTET,
                 // VEDTAK_FATTET_AUTOMATISK, que?
                 UENDRET_UTFALL, REGISTRER_OM_VERGE -> fraMaltype2(h, behandlingUUID);
            case SAK_GODKJENT, FAKTA_ENDRET, KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, UTTAK, TERMINBEKREFTELSE_UGYLDIG, ANKE_BEH ->
                fraMalType5(h, behandlingUUID);
            case OVST_UTTAK, FASTSATT_UTTAK -> fraMaltype10(h, behandlingUUID);
            default -> null; //TODO fjerne default
        };
    }

    private static HistorikkinnslagDtoV2 fraMaltype10(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var tittel = "TODO";
        var tekster = new ArrayList<String>();
        for(var del : h.getHistorikkinnslagDeler()) {
            String opplysningTekst;
            if (h.getType().equals(HistorikkinnslagType.OVST_UTTAK)) {
                var tekst = "<b>Overstyrt vurdering</b> av perioden {periodeFom} - {periodeTom}.";

                var periodeFom = del.getOpplysninger().stream()
                    .filter(o -> UTTAK_PERIODE_FOM.getKode().equals(o.getNavn()))
                    .map(HistorikkinnslagFelt::getTilVerdi)
                    .findFirst()
                    .orElse("");

                var periodeTom = del.getOpplysninger().stream()
                    .filter(o -> UTTAK_PERIODE_TOM.getKode().equals(o.getNavn()))
                    .map(HistorikkinnslagFelt::getTilVerdi)
                    .findFirst()
                    .orElse("");

                tekst.replace("{periodeFom}", periodeFom);
                tekst.replace("{periodeTom}", periodeTom);
                opplysningTekst = tekst;
            }
            if (h.getType().equals(HistorikkinnslagType.FASTSATT_UTTAK)) {
                // opplysing
                var tekst = "<b>Manuell vurdering</b> av perioden {periodeFom} - {periodeTom}.";
                var periodeFom = del.getOpplysninger().stream()
                    .filter(o -> UTTAK_PERIODE_FOM.getKode().equals(o.getNavn()))
                    .map(HistorikkinnslagFelt::getTilVerdi)
                    .findFirst()
                    .orElse("");

                var periodeTom = del.getOpplysninger().stream()
                    .filter(o -> UTTAK_PERIODE_TOM.getKode().equals(o.getNavn()))
                    .map(HistorikkinnslagFelt::getTilVerdi)
                    .findFirst()
                    .orElse("");
                tekst.replace("{periodeFom}", periodeFom);
                tekst.replace("{periodeTom}", periodeTom);
                opplysningTekst = tekst;
            }

            // Endret felt
            var tekst = "<b>{fieldName}</b> er endret fra {fromValueWeeks} uker og {fromValueDays} dager til <b>{toValueWeeks} uker og {toValueDays} dager</b>";
            var fieldName = kodeverdiTilStreng(HistorikkEndretFeltType.fraKode(felt.getNavn()), felt.getNavnVerdi());
            var fromValueWeeks = ;
            var fromValueDays = ;
            var toValueWeeks = ;
            var toValueDays = ;

            tekster.add();
        }

        return new HistorikkinnslagDtoV2(
            behandlingUUID,
            HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null),
            h.getOpprettetTidspunkt(),
            null, // TODO
            tittel,
            tekster);
    }

    private static HistorikkinnslagDtoV2 fraMalType5(Historikkinnslag h, UUID behandlingUUID) {
        var skjermlenke = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getSkjermlenke().stream())
            .map(SkjermlenkeType::fraKode)
            .findFirst();
        var lenker = tilDto(h.getDokumentLinker());
        var tittel = switch (h.getType()) {
            case KLAGE_BEH_NK, KLAGE_BEH_NFP, BYTT_ENHET, ANKE_BEH -> h.getType().getNavn();
            default -> null;
        };
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()),
            skjermlenke.orElse(null), h.getOpprettetTidspunkt(), lenker, tittel, lagTekstForMal5(h));
    }

    /**
     * Slik er det nå (dette er feil):
     * Hendelse 1
     * Hendelse 2
     * Resultat 1
     * Resultat 2
     *
     * Dette er riktig:
     * Hendelse 1
     * Resultat 1
     * Hendelse 2
     * Resultat 2
     */
    private static List<String> lagTekstForMal5(Historikkinnslag h) {
        var hendelseTekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getHendelse().stream())
            .map(HistorikkinnslagFelt::getNavn)
            .map(HistorikkinnslagType::fraKode)
            .map(HistorikkinnslagType::getNavn)
            .toList();

        var resultatTekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getResultat().stream())
            .map(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter::fraHistorikkResultat)
            .toList();

        var gjeldendeFraInnslag = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getGjeldendeFraFelt().stream().map(felt -> tilGjeldendeFraInnslag(felt, del)))
            .toList();

        var søknadsperiode = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getAvklartSoeknadsperiode().stream())
            .map(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter::fraSøknadsperiode)
            .toList();

        var tema = h.getHistorikkinnslagDeler().stream().flatMap(del -> del.getTema().stream()).map(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter::fraTema).toList();

        var endretFelter = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getEndredeFelt().stream())
            .map(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter::fraEndretFelt)
            .toList();

        var opplysninger = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getOpplysninger().stream())
            .map(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter::fraOpplysning)
            .toList();

        var årsaktekst = h.getHistorikkinnslagDeler()
            .stream()
            .flatMap(del -> del.getAarsakFelt().stream())
            .flatMap(felt -> finnÅrsakKodeListe(felt).stream())
            .map(Kodeverdi::getNavn)
            .toList();

        var begrunnelsetekst = h.getHistorikkinnslagDeler().stream().flatMap(d -> begrunnelseFraDel(d).stream()).toList();

        var body = new ArrayList<>(hendelseTekst);
        body.addAll(hendelseTekst);
        body.addAll(resultatTekst);
        body.addAll(gjeldendeFraInnslag);
        body.addAll(søknadsperiode);
        body.addAll(tema);
        body.addAll(endretFelter);
        body.addAll(opplysninger);
        body.addAll(årsaktekst);
        body.addAll(begrunnelsetekst);
        return body;
    }

    private static String fraOpplysning(HistorikkinnslagFelt opplysning) {
        var historikkOpplysningType = HistorikkOpplysningType.fraKode(opplysning.getNavn());
        var tekst = switch (historikkOpplysningType) {
            case ANTALL_BARN -> "<b>Antall barn</b> som brukes i behandlingen: <b>{antallBarn}</b>";
            case TPS_ANTALL_BARN -> "Antall barn";
            case FODSELSDATO -> "Når ble barnet født?";
            case UTTAK_PERIODE_FOM -> historikkOpplysningType.getNavn(); // TODO: ingen feltid frontend?
            case UTTAK_PERIODE_TOM -> historikkOpplysningType.getNavn(); // TODO: ingen feltid frontend?
            default -> throw new IllegalStateException("Unexpected value: " + historikkOpplysningType);
        };
        return tekst.replace("{antallBarn}", opplysning.getTilVerdi());
    }

    private static String fraEndretFelt(HistorikkinnslagFelt felt) {
        var endretendretFeltNavnFeltNavn = HistorikkEndretFeltType.fraKode(felt.getNavn());

        var feltNavn = kodeverdiTilStreng(endretendretFeltNavnFeltNavn, felt.getNavnVerdi());
        var tilVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getTilVerdiKode(), felt.getTilVerdiKode());

        if (felt.getFraVerdi() == null || endretendretFeltNavnFeltNavn.equals(HistorikkEndretFeltType.FORDELING_FOR_NY_ANDEL)) {
            return String.format("<b>%s</b> er satt til <b>%s</b>.", feltNavn, tilVerdi);
        } else {
            var fraVerdi = kodeverdiTilStrengEndretFeltTilverdi(felt.getFraVerdiKode(), felt.getFraVerdi());
            return String.format("<b>%s</b> endret fra %s til <b>%s</b>", feltNavn, fraVerdi, tilVerdi);
        }
    }

    private static String kodeverdiTilStrengEndretFeltTilverdi(String verdiKode, String verdi) {
        if (verdiKode == null) {
            return verdi;
        }

        return switch (verdiKode) {
            case "INNVILGET" -> "Oppfylt";
            case "AVSLÅTT" -> "Ikke oppfylt";
            case "2003" -> "Innvilget uttak av kvote";
            case "VILKAR_OPPFYLT" -> "Vilkåret er oppfylt";
            case "2011" -> "HistorikkEndretFeltVerdiType.GyldigUtsettelsePgaArbeid";
            case "ARBEIDSTAKER" -> "Arbeidstaker";
            case "I_AKTIVITET" -> ""; // FIXME Thao: Finner ikke denne
            case "2002" -> "Innvilget fellesperiode/foreldrepenger";
            case "2031" -> "Gradering av kvote/overført kvote";
            case "FEDREKVOTE" -> "Fedrekvote";
            case "FELLESPERIODE" -> "Fellesperiode";
            case "2038" -> "Redusert uttaksgrad pga. den andre forelderens uttak";
            case "LOVLIG_OPPHOLD" -> "Søker har lovlig opphold";
            case "MØDREKVOTE" -> "Mødrekvote";
            case "4002" -> "Ikke stønadsdager igjen";
            case "GRADERING_OPPFYLT" -> "Oppfylt";
            case "2004" -> "Innvilget foreldrepenger, kun far har rett";
            case "2030" -> "Gradering av fellesperiode/foreldrepenger";
            case "ANNEN_FORELDER_HAR_IKKE_RETT" -> "Annen forelder har ikke rett";
            case "TILBAKEKR_OPPRETT" -> "Opprett tilbakekreving";
            case "FASTSETT_RESULTAT_PERIODEN_AVKLARES_IKKE" -> "Perioden kan ikke avklares";
            case "FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT" -> "Sykdommen/skaden er dokumentert";
            case "HAR_GYLDIG_GRUNN" -> "Gyldig grunn for sen fremsetting av søknaden";
            case "BRUK_MED_OVERSTYRTE_PERIODER" -> "Bruk arbeidsforholdet med overstyrt periode";
            case "GRADERING_IKKE_OPPFYLT" -> "Ikke oppfylt";
            case "2010" -> "Gyldig utsettelse pga ferie";
            case "IKKE_BENYTT" -> "Ikke benytt";
            case "VARIG_ENDRET_NAERING" -> "Varig endret eller nystartet næring";
            case "FORELDREPENGER" -> "Foreldrepenger";
            case "INGEN_INNVIRKNING" -> "Faresignalene hadde ingen innvirkning på behandlingen";
            case "2037" -> "Innvilget fellesperiode til far";
            case "VILKAR_IKKE_OPPFYLT" -> "Vilkåret er ikke oppfylt";
            case "FORTSETT_UTEN_INNTEKTSMELDING" -> "Gå videre uten inntektsmelding";
            case "MANGLENDE_OPPLYSNINGER" -> "Benytt i behandlingen, men har manglende opplysninger";
            case "IKKE_OPPFYLT" -> "ikke oppfylt";
            case "BENYTT" -> "Benytt";
            case "KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING" -> "Arbeidsgiver kontaktes";
            case "FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT" -> "Innleggelsen er dokumentert";
            case "TILBAKEKR_IGNORER" -> "Avvent samordning, ingen tilbakekreving";
            case "SELVSTENDIG_NÆRINGSDRIVENDE" -> "Selvstendig næringsdrivende";
            case "2016" -> "Utsettelse pga. 100% arbeid, kun far har rett";
            case "2033" -> "Gradering foreldrepenger, kun far har rett";
            case "4084" -> "Annen part har overlappende uttak, det er ikke søkt/innvilget samtidig uttak";
            case "INNTEKT_IKKE_MED_I_BG" -> "Benytt i behandligen. Inntekten er ikke med i beregningsgrunnlaget";
            case "2021" -> "Overføring oppfylt, annen part er helt avhengig av hjelp til å ta seg av barnet";
            case "OPPFYLT" -> "oppfylt";
            case "SØKER_ER_IKKE_I_PERMISJON" -> "Søker er ikke i permisjon";
            case "BOSATT_I_NORGE" -> "Søker er bosatt i Norge";
            case "INNVIRKNING" -> "Faresignalene hadde innvirkning på behandlingen";
            case "OPPHOLDSRETT" -> "Søker har ikke oppholdsrett (EØS)";
            case "2036" -> "Innvilget foreldrepenger, kun far har rett og mor er ufør";
            case "BOSATT_UTLAND" -> "Bosatt utland";
            case "SØKER_ER_I_PERMISJON" -> "Søker er i permisjon";
            case "IKKE_BOSATT_I_NORGE" -> "Søker er ikke bosatt i Norge";
            case "FRILANSER" -> "Frilanser";
            case "2006" -> "Innvilget foreldrepenger før fødsel";
            case "ANNEN_FORELDER_HAR_RETT" -> "Annen forelder har rett";
            case "4060" -> "Samtidig uttak - ikke gyldig kombinasjon";
            case "DAGPENGER" -> "Dagpenger";
            case "INGEN_VARIG_ENDRING_NAERING" -> "Ingen varig endret eller nyoppstartet næring";
            case "IKKE_BRUK" -> "Ikke bruk";
            case "IKKE_TIDSBEGRENSET_ARBEIDSFORHOLD" -> "ikke tidsbegrenset";
            case "4076" -> "Avslag overføring av kvote pga. annen forelder har rett til foreldrepenger";
            case "HINDRE_TILBAKETREKK" -> "Ikke tilbakekrev fra søker";
            case "4034" -> "Avslag utsettelse - ingen stønadsdager igjen";
            case "FORELDREPENGER_FØR_FØDSEL" -> "Foreldrepenger før fødsel";
            case "IKKE_I_AKTIVITET_DOKUMENTERT" -> ""; //FIXME Thao: Finner ikke denne
            case "IKKE_I_AKTIVITET_IKKE_DOKUMENTERT" -> ""; //FIXME Thao: Finner ikke denne
            case "BENYTT_A_INNTEKT_I_BG" -> "Benytt i behandlingen. Inntekt fra A-inntekt benyttes i beregningsgrunnlaget";
            case "4005" -> "Hull mellom stønadsperioder";
            case "IKKE_NY_I_ARBEIDSLIVET" -> "ikke ny i arbeidslivet";
            case "4050" -> "Aktivitetskravet arbeid ikke oppfylt";
            case "4082" -> "Avslag utsettelse pga arbeid tilbake i tid";
            case "TIDSBEGRENSET_ARBEIDSFORHOLD" -> "tidsbegrenset";
            case "4035" -> "Far aleneomsorg, mor fyller ikke aktivitetskravet";
            case "4051" -> "Aktivitetskravet offentlig godkjent utdanning ikke oppfylt";
            case "HAR_IKKE_GYLDIG_GRUNN" -> "Ingen gyldig grunn for sen fremsetting av søknaden";
            case "EØS_BOSATT_NORGE" -> "EØS bosatt Norge";
            case "4066" -> "Aktivitetskrav- arbeid ikke dokumentert";
            case "4067" -> "Aktivitetskrav – utdanning ikke dokumentert";
            case "FASTSETT_RESULTAT_GRADERING_AVKLARES" -> "Perioden er ok";
            case "2020" -> "Overføring oppfylt, annen part har ikke rett til foreldrepengene";
            case "2024" -> "Gyldig utsettelse";
            case "4040" -> "Barnets innleggelse ikke oppfylt";
            case "NY_I_ARBEIDSLIVET" -> "ny i arbeidslivet";
            case "2013" -> "Gyldig utsettelse pga barn innlagt";
            case "2023" -> "Overføring oppfylt, søker har aleneomsorg for barnet";
            case "2007" -> "Innvilget foreldrepenger, kun mor har rett";
            case "4020" -> "Brudd på søknadsfrist";
            case "2014" -> "Gyldig utsettelse pga sykdom";
            case "4023" -> "Arbeider i uttaksperioden mer enn 0%";
            case "4086" -> "Annen part har overlappende uttaksperioder som er innvilget utsettelse";
            case "FASTSETT_RESULTAT_ENDRE_SOEKNADSPERIODEN" -> "Endre søknadsperioden";
            case "UTFØR_TILBAKETREKK" -> "Tilbakekrev fra søker";
            case "FASTSETT_RESULTAT_PERIODEN_SYKDOM_DOKUMENTERT_IKKE" -> "Sykdommen/skaden er ikke dokumentert";
            case "NASJONAL" -> "Nasjonal";
            case "4053" -> "Aktivitetskravet mors sykdom/skade ikke oppfylt";
            case "2028" -> "Bare far rett, aktivitetskravet oppfylt";
            case "4012" -> "Far har ikke omsorg";
            case "4501" -> "Ikke gradering pga. for sen søknad"; // TODO Thao: Fant bare dette i kodeverk. Holder det?
            case "2015" -> "Utsettelse pga. ferie, kun far har rett";
            case "IKKE_LOVLIG_OPPHOLD" -> "Søker har ikke lovlig opphold";
            case "2034" -> "Gradering foreldrepenger, kun mor har rett";
            case "ARBEIDSAVKLARINGSPENGER" -> "Arbeidsavklaringspenger";
            case "2005" -> "Innvilget foreldrepenger ved aleneomsorg";
            case "4503" -> "Avslag gradering - ikke rett til gradert uttak pga. redusert oppfylt aktivitetskrav på mor";
            case "4037" -> "Ikke heltidsarbeid";
            case "4102" -> "Bare far har rett, mangler søknad uttak/aktivitetskrav";
            case "FASTSETT_RESULTAT_PERIODEN_INNLEGGELSEN_DOKUMENTERT_IKKE" -> "Innleggelsen er ikke dokumentert";
            case "4030" -> "Avslag utsettelse før termin/fødsel";
            case "4061" -> "Utsettelse ferie ikke dokumentert";
            case "IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING" -> "Ikke opprett arbeidsforhold";
            case "4069" -> "Aktivitetskrav – sykdom/skade ikke dokumentert";
            case "4007" -> "Den andre part syk/skadet ikke oppfylt";
            case "2035" -> "Gradering foreldrepenger, kun far har rett - dager uten aktivitetskrav";
            case "IKKE_ALENEOMSORG" -> "Søker har ikke aleneomsorg for barnet";
            case "4077" -> "Innvilget prematuruker, med fratrekk pleiepenger";
            case "4022" -> "Barnet er over 3 år";
            case "FORTSETT_BEHANDLING" -> "Fortsett behandling";
            case "4074" -> "Avslag overføring av kvote pga. sykdom/skade/innleggelse er ikke dokumentert";
            case "4062" -> "Utsettelse arbeid ikke dokumentert";
            case "JORDBRUKER" -> "Selvstendig næringsdrivende - Jordbruker";
            case "ALENEOMSORG" -> "Søker har aleneomsorg for barnet";
            case "4025" -> "Avslag gradering - arbeid 100% eller mer";
            case "IKKE_NYOPPSTARTET" -> "ikke nyoppstartet";
            case "4092" -> "Avslag overføring - har ikke aleneomsorg for barnet";
            case "4063" -> "Utsettelse søkers sykdom/skade ikke dokumentert";
            case "4055" -> "Aktivitetskravet mors deltakelse på introduksjonsprogram ikke oppfylt";
            case "4072" -> "Barnet er dødt";
            case "4073" -> "Ikke rett til kvote fordi mor ikke har rett til foreldrepenger";
            case "4038" -> "Søkers sykdom/skade ikke oppfylt";
            case "BOSA" -> "Bosatt";
            case "4085" -> "Det er ikke samtykke mellom partene";
            case "4033" -> "Ikke lovbestemt ferie";
            case "2032" -> "Gradering foreldrepenger ved aleneomsorg";
            case "4095" -> "Mor tar ikke alle 3 ukene før termin";
            case "NYOPPSTARTET" -> "nyoppstartet";
            case "4087" -> "Opphør av medlemskap";
            case "MEDLEM" -> "Periode med medlemskap";
            case "2017" -> "Utsettelse pga. sykdom, skade, kun far har rett";
            case "4104" -> "Stønadsperiode for nytt barn";
            case "LAGT_TIL_AV_SAKSBEHANDLER" -> "Arbeidsforholdet er lagt til av saksbehandler";
            case "ARBEIDSTAKER_UTEN_FERIEPENGER" -> "Arbeidstaker uten feriepenger";
            case "4081" -> "Avslag utsettelse pga ferie tilbake i tid";
            case "OPPRETT_BASERT_PÅ_INNTEKTSMELDING" -> "Opprettet basert på inntektsmeldingen";
            case "2026" -> "Gyldig utsettelse første 6 uker pga. barn innlagt";
            case "4031" -> "Ferie innenfor de første 6 ukene";
            case "4052" -> "Aktivitetskravet offentlig godkjent utdanning i kombinasjon med arbeid ikke oppfylt";
            case "2019" -> "Utsettelse pga. barnets innleggelse på helseinstitusjon, kun far har rett";
            case "4056" -> "Aktivitetskravet mors deltakelse på kvalifiseringsprogram ikke oppfylt";
            case "4112" -> "Barnets innleggelse første 6 uker ikke oppfylt";
            case "SJØMANN" -> "Arbeidstaker - Sjømann";
            case "2039" -> "Innvilget første 6 uker etter fødsel";
            case "4041" -> "Avslag utsettelse ferie på bevegelig helligdag";
            case "IKKE_OPPHOLDSRETT" -> "Søker har ikke oppholdsrett (EØS)";
            case "2022" -> "Overføring oppfylt, annen part er innlagt i helseinstitusjon";
            case "FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT" -> "Øvelse eller tjeneste i heimevernet er dokumentert";
            case "4098" -> "Opphør av foreldreansvarvilkåret";
            case "4065" -> "Utsettelse barnets innleggelse - barnets innleggelse ikke dokumentert";
            case "2027" -> "Gyldig utsettelse første 6 uker pga. sykdom";
            case "UTVA" -> "Utvandret";
            case "4093" -> "Avslag gradering - søker er ikke i arbeid";
            case "4068" -> "Aktivitetskrav – arbeid i kombinasjon med utdanning ikke dokumentert";
            case "MANUELT_OPPRETTET_AV_SAKSBEHANDLER" -> "Opprettet av saksbehandler";
            case "4502" -> "Avslag graderingsavtale mangler - ikke dokumentert"; // TODO Thao: Fant bare dette i kodeverk. Holder det?
            case "2012" -> "Gyldig utsettelse pga innleggelse";
            case "4003" -> "Mor har ikke omsorg";
            case "4057" -> "Unntak for aktivitetskravet, mors mottak av uføretrygd ikke oppfylt";
            case "FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT" -> "Tiltak i regi av NAV er dokumentert";
            case "4088" -> "Aktivitetskrav – introduksjonsprogram ikke dokumentert";
            case "FISKER" -> "Selvstendig næringsdrivende - Fisker";
            case "4523" -> "Avslag gradering - arbeid 100% eller mer";
            case "PRAKSIS_UTSETTELSE" -> "Feil praksis utsettelse";
            case "4107" -> "Ikke nok dager uten aktivitetskrav";
            case "4504" -> "Avslag gradering - gradering før uke 7";
            case "GRADERING_PÅ_ANDEL_UTEN_BG_IKKE_SATT_PÅ_VENT" -> "Riktig";
            case "FASTSETT_RESULTAT_PERIODEN_NAV_TILTAK_DOKUMENTERT_IKKE" -> "Tiltak i regi av NAV er ikke dokumentert";
            case "KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_ARBEIDSFORHOLD" -> "Arbeidsgiver kontaktes";
            case "4100" -> "Uttak før omsorgsovertakelse";
            case "4075" -> "Ikke rett til fellesperiode fordi mor ikke har rett til foreldrepenger";
            case "IKKE_RELEVANT" -> "Ikke relevant periode";
            case "4115" -> "Søkers sykdom/skade første 6 uker ikke dokumentert";
            case "4103" -> "Mangler søknad for første 6 uker etter fødsel";
            case "4099" -> "Opphør av opptjeningsvilkåret";
            case "4089" -> "Aktivitetskrav – kvalifiseringsprogrammet ikke dokumentert";
            case "4106" -> "Far/medmor søker mer enn 10 dager ifm fødsel";
            case "4117" -> "Barnets innleggelse første 6 uker ikke dokumentert";
            case "4070" -> "Aktivitetskrav – innleggelse ikke dokumentert";
            case "4032" -> "Ferie - selvstendig næringsdrivende/frilanser";
            case "4110" -> "Søkers sykdom/skade første 6 uker ikke oppfylt";
            case "4096" -> "Opphør av fødselsvilkåret";
            case "FORELDREANSVAR_4_TITTEL" -> "Foreldreansvarsvilkåret § 14-17 fjerde ledd";
            case "2018" -> "Utsettelse pga. egen innleggelse på helseinstitusjon, kun far har rett";
            case "4008" -> "Den andre part innleggelse ikke oppfylt";
            case "FJERN_FRA_BEHANDLINGEN" -> "Fjernet fra behandlingen";
            case "4054" -> "Aktivitetskravet mors innleggelse ikke oppfylt";
            case "4097" -> "Opphør av adopsjonsvilkåret";
            case "4064" -> "Utsettelse søkers innleggelse ikke dokumentert";
            case "DAGMAMMA" -> "Selvstendig næringsdrivende - Dagmamma";
            case "DOKUMENTERT" -> "dokumentert";
            case "2025" -> "Gyldig utsettelse første 6 uker pga. innleggelse";
            case "4059" -> "Unntak for aktivitetskravet, flerbarnsdager - ikke nok dager";
            case "IKKE_EKTEFELLES_BARN" -> "ikke ektefelles barn";
            case "4105" -> "Far/medmor søker uttak før fødsel/omsorg";
            case "HENLEGG_BEHANDLING" -> "Henlegg behandling";
            case "SAMMENSATT_KONTROLL" -> "Sammensatt kontroll";
            case "NYTT_ARBEIDSFORHOLD" -> "Arbeidsforholdet er ansett som nytt";
            case "4013" -> "Mor søker fellesperiode før 12 uker før termin/fødsel";
            case "4071" -> "Aktivitetskrav – introduksjonsprogram ikke dokumentert";
            case "ADOPTERER_IKKE_ALENE" -> "adopterer ikke alene";
            case "IKKE_DOKUMENTERT" -> "ikke dokumentert";
            case "EKTEFELLES_BARN" -> "ektefelles barn";
            case "4039" -> "Søkers innleggelse ikke oppfylt";
            case "4111" -> "Søkers innleggelse første 6 uker ikke oppfylt";
            case "FORELDREANSVAR_2_TITTEL" -> "Foreldreansvarsvilkåret § 14-17 andre ledd";
            case "UNNTAK" -> "Perioder uten medlemskap";
            case "4058" -> "Unntak for aktivitetskravet, stebarnsadopsjon - ikke nok dager";
            case "4116" -> "Søkers innleggelse første 6 uker ikke dokumentert";
            case "OMSORGSVILKARET_TITTEL" -> "Omsorgsvilkår § 14-17 tredje ledd";
            case "ADOPTERER_ALENE" -> "adopterer alene";
            case "FASTSETT_RESULTAT_PERIODEN_HV_DOKUMENTERT_IKKE" -> "Øvelse eller tjeneste i heimevernet er ikke dokumentert";
            case "VERGE" -> "Verge/fullmektig";
            default -> throw new IllegalStateException("Unexpected value: " + felt);
        };
    }

    private static String kodeverdiTilStreng(HistorikkEndretFeltType endretFeltNavn, String verdi) {
        var tekstFrontend = switch (endretFeltNavn) {
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
            case EKTEFELLES_BARN -> "Ektefelles/samboers barn";
            case ENDRING_NAERING -> "Endring i næringsvirksomhet";
            case ENDRING_TIDSBEGRENSET_ARBEIDSFORHOLD -> "Arbeidsforhold hos {value}";
            case ER_SOKER_BOSATT_I_NORGE -> "Bosted";
            case FODSELSVILKARET -> "Fødselsvilkåret";
            case FODSELSDATO -> "Fødselsdato";
            case FORDELING_FOR_ANDEL -> "{value}";
            case FORDELING_FOR_NY_ANDEL -> "{value}";
            case FORDELING_ETTER_BESTEBEREGNING -> "Fordeling etter besteberegning";
            case FORELDREANSVARSVILKARET -> "Foreldreansvarsvilkåret";
            case FRILANS_INNTEKT -> "Frilansinntekt";
            case FRILANSVIRKSOMHET -> "Frilansvirksomhet";
            case GYLDIG_MEDLEM_FOLKETRYGDEN -> "Vurder om søker har gyldig medlemskap i perioden";
            case INNTEKT_FRA_ARBEIDSFORHOLD -> "Inntekt fra {value}";
            case LØNNSENDRING_I_PERIODEN -> "Lønnsendring siste tre måneder";
            case MANN_ADOPTERER -> "Mann adopterer";
            case MOTTAR_YTELSE_ARBEID -> "Mottar søker ytelse for arbeid i {value}";
            case MOTTAR_YTELSE_FRILANS -> "Mottar søker ytelse for frilansaktiviteten";
            case MOTTATT_DATO -> "Dato for når søknaden kan anses som mottatt";
            case OMSORG -> "Omsorg";
            case OMSORGSOVERTAKELSESDATO -> "Omsorgsovertakelsesdato";
            case OMSORGSVILKAR -> "Omsorgsvilkåret";
            case IKKE_OMSORG_PERIODEN -> "Søker har ikke omsorg for barnet i perioden";
            case INNTEKTSKATEGORI_FOR_ANDEL -> "Inntektskategori for {value}";
            case OPPHOLDSRETT_EOS -> "Oppholdsrett";
            case OPPHOLDSRETT_IKKE_EOS -> "Lovlig opphold";
            case OVERSTYRT_BEREGNING -> "Overstyrt beregning:; Beløpet er endret fra";
            case OVERSTYRT_VURDERING -> "Overstyrt vurdering:; Utfallet er endret fra";
            case SELVSTENDIG_NÆRINGSDRIVENDE -> "Selvstendig næringsdrivende";
            case SOKERSOPPLYSNINGSPLIKT -> "Søkers opplysningsplikt";
            case SVANGERSKAPSPENGERVILKÅRET -> "Svangerskapsvilkåret";
            case SOKNADSFRIST -> "Søknadsfrist";
            case SOKNADSFRISTVILKARET -> "Søknadsfristvilkåret";
            case STARTDATO_FRA_SOKNAD -> "Startdato fra søknad";
            case TERMINBEKREFTELSE -> "Terminbekreftelse";
            case TERMINDATO -> "Termindato";
            case UTSTEDTDATO -> "Utstedt dato";
            case VILKAR_SOM_ANVENDES -> "Vilkår som anvendes";
            case FASTSETT_RESULTAT_PERIODEN -> "Resultat for perioden";
            case AVKLART_PERIODE -> "Avklart periode";
            case ANDEL_ARBEID -> "Andel i arbeid";
            case UTTAK_TREKKDAGER -> "Trekk";
            case UTTAK_STØNADSKONTOTYPE -> "Stønadskonto";
            case UTTAK_PERIODE_RESULTAT_TYPE -> "Resultatet";
            case UTTAK_PROSENT_UTBETALING -> "Utbetalingsgrad";
            case UTTAK_SAMTIDIG_UTTAK -> "Samtidig uttak";
            case UTTAK_TREKKDAGER_FLERBARN_KVOTE -> "Flerbarnsdager";
            case UTTAK_PERIODE_RESULTAT_ÅRSAK -> "Årsak resultat";
            case UTTAK_GRADERING_ARBEIDSFORHOLD -> "Gradering av arbeidsforhold";
            case UTTAK_GRADERING_AVSLAG_ÅRSAK -> "Årsak avslag gradering";
            case UTTAK_SPLITT_TIDSPERIODE -> endretFeltNavn.getNavn(); // Ikke i frontend
            case SYKDOM -> "Sykdom";
            case ARBEIDSFORHOLD -> "Arbeidsforhold hos {value}";
            case NY_FORDELING -> "Ny fordeling <b>{value}</b>";
            case NY_AKTIVITET -> "Det er lagt til ny aktivitet for <b>{value}</b>";
            case NYTT_REFUSJONSKRAV -> "Nytt refusjonskrav";
            case INNTEKT -> "Inntekt fra {value}";
            case INNTEKTSKATEGORI -> "Inntektskategori";
            case NAVN -> "Navn";
            case FNR -> "Fødselsnummer";
            case PERIODE_FOM -> "Periode f.o.m.";
            case PERIODE_TOM -> "Periode t.o.m.";
            case MANDAT -> "Mandat";
            case KONTAKTPERSON -> "Kontaktperson";
            case BRUKER_TVUNGEN -> "Søker er under tvungen forvaltning";
            case TYPE_VERGE -> "Type verge";
            case DAGPENGER_INNTEKT -> "Dagpenger";
            case KLAGE_RESULTAT_NFP -> "Resultat";
            case KLAGE_RESULTAT_KA -> "Ytelsesvedtak";
            case KLAGE_OMGJØR_ÅRSAK -> "Årsak til omgjøring";
            case ER_KLAGER_PART -> "Er klager part i saken";
            case ER_KLAGE_KONKRET -> "Klages det på konkrete elementer i vedtaket";
            case ER_KLAGEFRIST_OVERHOLDT -> "Er klagefristen overholdt";
            case ER_KLAGEN_SIGNERT -> "Er klagen signert";
            case PA_KLAGD_BEHANDLINGID -> "Vedtaket som er påklagd";
            case ANKE_RESULTAT -> "Vedtaket som er anket";
            case KONTROLL_AV_BESTEBEREGNING -> "Godkjenning av automatisk besteberegning";
            case ANKE_OMGJØR_ÅRSAK -> "Omgjøringsårsak";
            case ER_ANKER_IKKE_PART -> "Er anker ikke part";
            case ER_ANKE_IKKE_KONKRET -> "Er anke ikke konkret";
            case ER_ANKEFRIST_IKKE_OVERHOLDT -> "Er ankefrist ikke overholdt";
            case ER_ANKEN_IKKE_SIGNERT -> "Er anken ikke signer";
            case PA_ANKET_BEHANDLINGID -> "Vedtaket som er påklagd";
            case VURDER_ETTERLØNN_SLUTTPAKKE -> "Inntekt fra etterlønn eller sluttpakke";
            case FASTSETT_ETTERLØNN_SLUTTPAKKE -> "Inntekten";
            case ER_VILKÅRENE_TILBAKEKREVING_OPPFYLT -> endretFeltNavn.getNavn(); // Ikke i frontend
            case ER_SÆRLIGE_GRUNNER_TIL_REDUKSJON -> endretFeltNavn.getNavn(); // Ikke i frontend
            case FASTSETT_VIDERE_BEHANDLING -> endretFeltNavn.getNavn(); // Ikke i frontend
            case RETT_TIL_FORELDREPENGER -> "Rett til foreldrepenger";
            case MOR_MOTTAR_UFØRETRYGD -> "Mor mottar uføretrygd";
            case MOR_MOTTAR_STØNAD_EØS -> endretFeltNavn.getNavn(); // Ikke i frontend
            case ANNEN_FORELDER_RETT_EØS -> "Annen forelder har tilstrekkelig opptjening fra land i EØS";
            case VURDER_GRADERING_PÅ_ANDEL_UTEN_BG -> "Inntektsgrunnlag ved gradering";
            case DEKNINGSGRAD -> "Dekningsgrad";
            case TILBAKETREKK -> "Tilbaketrekk";
            case SAKSMARKERING -> "Saksmarkering";
            case INNHENT_SED -> "Innhent dokumentasjon";
            case HEL_TILRETTELEGGING_FOM -> "Hel tilrettelegging fra og med";
            case DELVIS_TILRETTELEGGING_FOM -> "Delvis tilrettelegging fra og med";
            case STILLINGSPROSENT -> "stillingsprosent";
            case SLUTTE_ARBEID_FOM -> "Slutte arbeid fra og med";
            case TILRETTELEGGING_BEHOV_FOM -> "Tilrettelegging er nødvendig fra og med";
            case TILRETTELEGGING_SKAL_BRUKES -> "Tilrettelegging skal brukes";
            case FARESIGNALER -> "Resultat";
            case MILITÆR_ELLER_SIVIL -> "Har søker militær- eller siviltjeneste i opptjeningsperioden";
            case NY_REFUSJONSFRIST -> "Utvidelse av frist for fremsatt refusjonskrav for {value}";
            case NY_STARTDATO_REFUSJON -> "Startdato for refusjon til {value}";
            case DELVIS_REFUSJON_FØR_STARTDATO -> "Delvis refusjon før ";
            case ORGANISASJONSNUMMER -> "Organisasjonsnummer";
            case ARBEIDSFORHOLD_BEKREFTET_TOM_DATO -> endretFeltNavn.getNavn(); // Ikke i frontend
            case ANKE_AVVIST_ÅRSAK -> "Avvisningsårsak";
            case AKTIVITETSKRAV_AVKLARING -> endretFeltNavn.getNavn(); // Ikke i frontend
            case UTTAKPERIODE_DOK_AVKLARING -> "Perioden {value}";
            case FAKTA_UTTAK_PERIODE -> "Perioden {value}";
            case SVP_OPPHOLD_PERIODE -> "Periode med opphold";
            case VURDERT_ETTERBETALING_TIL_SØKER -> "Vurdering av etterbetaling til søker";
            case UDEFINIERT -> throw new IllegalStateException("UDEFINTER ");
        };
        if (tekstFrontend.contains("{value}")) {
            if (verdi == null) {
                LOG.info("historikkv2 manglende value - {} {}", endretFeltNavn, tekstFrontend);
            } else {
                return tekstFrontend.replace("{value}", verdi);
            }
        }
        return tekstFrontend;
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

        var tekst = switch (type) {
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
        return type.equals(HistorikkAvklartSoeknadsperiodeType.GRADERING) ? String.format(tekst, søknadsperiode.getNavnVerdi(),
            søknadsperiode.getTilVerdi()) : String.format(tekst, søknadsperiode.getTilVerdi());
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

        return String.format(endretFeltTekst, gjeldendeFra.getNavnVerdi()) + verditekst + (del.getEndredeFelt()
            .isEmpty() ? "Ingen endring av vurdering" : "");
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
            case ANKE_AVVIS, ANKE_OMGJOER, ANKE_OPPHEVE_OG_HJEMSENDE, ANKE_HJEMSENDE, ANKE_STADFESTET_VEDTAK, ANKE_DELVIS_OMGJOERING_TIL_GUNST,
                 ANKE_TIL_UGUNST, ANKE_TIL_GUNST -> historikkResultatType.getNavn(); // Ikke i frontend
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
        var begrunnelsetekst = begrunnelseFraDel(historikkinnslagDel).map(List::of);

        var body = begrunnelsetekst.orElse(List.of());
        return new HistorikkinnslagDtoV2(behandlingUUID, HistorikkinnslagDtoV2.HistorikkAktørDto.fra(innslag.getAktør(), innslag.getOpprettetAv()),
            skjermlenke, innslag.getOpprettetTidspunkt(), lenker, innslag.getType().getNavn(), body);
    }

    private static Optional<String> begrunnelseFraDel(HistorikkinnslagDel historikkinnslagDel) {
        return historikkinnslagDel.getBegrunnelseFelt()
            .flatMap(no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Adapter::finnÅrsakKodeListe)
            .map(Kodeverdi::getNavn)
            .or(historikkinnslagDel::getBegrunnelse);
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDto(List<HistorikkinnslagDokumentLink> dokumentLinker) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDto(d, null, null)) // TODO: husks aktiv journalpost og full URI
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDto(HistorikkinnslagDokumentLink lenke,
                                                          List<JournalpostId> journalPosterForSak,
                                                          URI dokumentPath) {
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(lenke.getLinkTekst());
        // dto.setUtgått(aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak));
        dto.setUtgått(true); //TODO
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        if (lenke.getJournalpostId().getVerdi() != null && lenke.getDokumentId() != null && dokumentPath != null) {
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
