package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivFilType;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalMetadata;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalTjeneste;
import no.nav.foreldrepenger.domene.person.tps.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
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
    private JournalTjeneste journalTjeneste;
    private PersoninfoAdapter personinfoAdapter;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository, JournalTjeneste journalTjeneste, PersoninfoAdapter personinfoAdapter) {
        this.historikkRepository = historikkRepository;
        this.journalTjeneste = journalTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    public void opprettHistorikkinnslag(Behandling behandling, JournalpostId journalpostId, Boolean selvOmLoggetTidligere) {
        if (!selvOmLoggetTidligere && historikkinnslagForBehandlingStartetErLoggetTidligere(behandling.getId(), HistorikkinnslagType.BEH_STARTET)) {
            return;
        }

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setKjoenn(setKjønn(behandling));
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        leggTilHistorikkinnslagDokumentlinker(behandling.getType(), journalpostId, historikkinnslag);

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

    void leggTilHistorikkinnslagDokumentlinker(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag) {
        List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            List<JournalMetadata> journalMetadataListe = journalTjeneste.hentMetadata(journalpostId);

            List<JournalMetadata> hoveddokumentJournalMetadata = journalMetadataListe.stream().filter(JournalMetadata::getErHoveddokument).collect(Collectors.toList());

            Optional<JournalMetadata> elektroniskSøknad = hoveddokumentJournalMetadata.stream()
                .filter(it -> VariantFormat.ORIGINAL.equals(it.getVariantFormat())
                    || VariantFormat.FULLVERSJON.equals(it.getVariantFormat())) //Ustrukturerte dokumenter kan ha xml med variantformat SKANNING_META
                .filter(it -> ArkivFilType.XML.equals(it.getArkivFilType())).findFirst();

            leggTilSøknadDokumentLenke(behandlingType, journalpostId, historikkinnslag, dokumentLinker, hoveddokumentJournalMetadata, elektroniskSøknad);
            journalMetadataListe.stream().filter(j -> !j.getErHoveddokument()).forEach(journalMetadata -> dokumentLinker
                .add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, VEDLEGG)));
        }

        historikkinnslag.setDokumentLinker(dokumentLinker);
    }

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag, List<HistorikkinnslagDokumentLink> dokumentLinker, List<JournalMetadata> hoveddokumentJournalMetadata, Optional<JournalMetadata> elektroniskSøknad) {
        if (elektroniskSøknad.isPresent()) {
            final JournalMetadata journalMetadata = elektroniskSøknad.get();
            String linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE :
                journalMetadata.getDokumentType().equals(DokumentTypeId.INNTEKTSMELDING) ? INNTEKTSMELDING : SØKNAD; // NOSONAR
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, linkTekst));
        } else {
            String linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : BehandlingType.UDEFINERT.equals(behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            Optional<JournalMetadata> papirSøknad = hoveddokumentJournalMetadata.stream().filter(j -> !ArkivFilType.XML.equals(j.getArkivFilType())).findFirst();
            papirSøknad.ifPresent(journalMetadata -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, linkTekst)));
        }
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(JournalMetadata journalMetadata, JournalpostId journalpostId, Historikkinnslag historikkinnslag, String linkTekst) {
        HistorikkinnslagDokumentLink historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(journalMetadata.getDokumentId());
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

    public void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId, DokumentTypeId dokumentTypeId) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        if (dokumentTypeId != null && dokumentTypeId.equals(DokumentTypeId.INNTEKTSMELDING)) {
            historikkinnslag.setAktør(HistorikkAktør.ARBEIDSGIVER);
        } else {
            historikkinnslag.setAktør(HistorikkAktør.SØKER);
        }
        historikkinnslag.setType(HistorikkinnslagType.VEDLEGG_MOTTATT);
        historikkinnslag.setFagsakId(fagsakId);

        leggTilHistorikkinnslagDokumentlinker(BehandlingType.UDEFINERT, journalpostId, historikkinnslag);

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
