package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.person.tps.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class HistorikkinnslagTjeneste {

    private static final String KLAGE = "Klage";
    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    private HistorikkRepository historikkRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private PersoninfoAdapter personinfoAdapter;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository,
                                    DokumentArkivTjeneste dokumentArkivTjeneste,
                                    PersoninfoAdapter personinfoAdapter) {
        this.historikkRepository = historikkRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    public void opprettHistorikkinnslag(Behandling behandling, JournalpostId journalpostId, Boolean selvOmLoggetTidligere, boolean elektronisk, boolean erIM) {
        if (!selvOmLoggetTidligere && historikkinnslagForBehandlingStartetErLoggetTidligere(behandling.getId(), HistorikkinnslagType.BEH_STARTET)) {
            return;
        }

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setKjoenn(setKjønn(behandling));
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        leggTilHistorikkinnslagDokumentlinker(behandling.getFagsak().getSaksnummer(), behandling.getType(), journalpostId, historikkinnslag, elektronisk, erIM);

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(BehandlingType.KLAGE.equals(behandling.getType()) ? HistorikkinnslagType.KLAGEBEH_STARTET : HistorikkinnslagType.BEH_STARTET);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    private NavBrukerKjønn setKjønn(Behandling behandling) {
        if (RelasjonsRolleType.UDEFINERT.equals(behandling.getFagsak().getRelasjonsRolleType())) {
            Personinfo personinfo = personinfoAdapter.innhentSaksopplysningerForSøker(behandling.getAktørId());
            if (personinfo != null) {
                return personinfo.getKjønn();
            }
        }
        return NavBrukerKjønn.UDEFINERT;
    }

    private boolean historikkinnslagForBehandlingStartetErLoggetTidligere(Long behandlingId, HistorikkinnslagType historikkinnslagType) {
        List<Historikkinnslag> eksisterendeHistorikkListe = historikkRepository.hentHistorikk(behandlingId);


        if (!eksisterendeHistorikkListe.isEmpty()) {
            for (Historikkinnslag eksisterendeHistorikk : eksisterendeHistorikkListe) {
                if (historikkinnslagType.equals(eksisterendeHistorikk.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    void leggTilHistorikkinnslagDokumentlinker(Saksnummer saksnummer, BehandlingType behandlingType, JournalpostId journalpostId,
                                               Historikkinnslag historikkinnslag, boolean elektronisk, boolean erIM) {
        List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            dokumentArkivTjeneste.hentJournalpostForSak(saksnummer, journalpostId).ifPresent(jp -> {
                leggTilSøknadDokumentLenke(behandlingType, journalpostId, historikkinnslag, dokumentLinker, jp.getHovedDokument(), elektronisk, erIM);
                jp.getAndreDokument().forEach(ad -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(ad, journalpostId, historikkinnslag, VEDLEGG)));
            });
        }

        historikkinnslag.setDokumentLinker(dokumentLinker);
    }

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag,
                                            List<HistorikkinnslagDokumentLink> dokumentLinker, ArkivDokument arkivDokument, boolean elektronisk, boolean erIM) {
        if (elektronisk) {
            String linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : (erIM ? INNTEKTSMELDING : SØKNAD); // NOSONAR
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, historikkinnslag, linkTekst));
        } else {
            String linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : BehandlingType.UDEFINERT.equals(behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            if (arkivDokument != null)
                dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, historikkinnslag, linkTekst));
        }
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(ArkivDokument arkivDokument, JournalpostId journalpostId, Historikkinnslag historikkinnslag, String linkTekst) {
        HistorikkinnslagDokumentLink historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(arkivDokument.getDokumentId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
        historikkinnslagDokumentLink.setHistorikkinnslag(historikkinnslag);
        return historikkinnslagDokumentLink;
    }

    public void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling){
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
            .medÅrsak(BehandlingResultatType.MERGET_OG_HENLAGT);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandling.getId());
        builder.build(historikkinnslag);
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForVedlegg(Fagsak fagsak, JournalpostId journalpostId, DokumentTypeId dokumentTypeId, boolean elektronisk) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        if (DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId)) {
            historikkinnslag.setAktør(HistorikkAktør.ARBEIDSGIVER);
        } else {
            historikkinnslag.setAktør(HistorikkAktør.SØKER);
        }
        historikkinnslag.setType(HistorikkinnslagType.VEDLEGG_MOTTATT);
        historikkinnslag.setFagsakId(fagsak.getId());

        leggTilHistorikkinnslagDokumentlinker(fagsak.getSaksnummer(), BehandlingType.UDEFINERT, journalpostId, historikkinnslag,
            elektronisk, DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId));

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDLEGG_MOTTATT);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();
        builder.medHendelse(historikkinnslagType);
        if (frist != null) {
            builder.medHendelse(historikkinnslagType, frist.toLocalDate());
        }
        if (!Venteårsak.UDEFINERT.equals(venteårsak)) {
            builder.medÅrsak(venteårsak);
        }
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(behandlingÅrsakType);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndringshendelse(Fagsak fagsak, String begrunnelse) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        historikkinnslag.setFagsakId(fagsak.getId());

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(begrunnelse);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndretStartdatoEtterFødselshendelse(Behandling behandling, LocalDate endretFra, LocalDate endretTil) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP)
            .medEndretFelt(HistorikkEndretFeltType.STARTDATO_FRA_SOKNAD, endretFra, endretTil);

        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
