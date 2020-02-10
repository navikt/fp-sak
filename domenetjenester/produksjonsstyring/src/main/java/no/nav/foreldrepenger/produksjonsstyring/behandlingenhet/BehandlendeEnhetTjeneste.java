package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event.BehandlingEnhetEventPubliserer;

@ApplicationScoped
public class BehandlendeEnhetTjeneste {

    private TpsTjeneste tpsTjeneste;
    private EnhetsTjeneste enhetsTjeneste;
    private BehandlingEnhetEventPubliserer eventPubliserer;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private PersonopplysningRepository personopplysningRepository;
    private HistorikkRepository historikkRepository;

    public BehandlendeEnhetTjeneste() {
        // For CDI
    }

    @Inject
    public BehandlendeEnhetTjeneste(TpsTjeneste tpsTjeneste,
                                    EnhetsTjeneste enhetsTjeneste,
                                    BehandlingEnhetEventPubliserer eventPubliserer,
                                    BehandlingRepositoryProvider provider) {
        this.tpsTjeneste = tpsTjeneste;
        this.enhetsTjeneste = enhetsTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.personopplysningRepository = provider.getPersonopplysningRepository();
        this.fagsakRelasjonRepository = provider.getFagsakRelasjonRepository();
        this.familieGrunnlagRepository = provider.getFamilieHendelseRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.historikkRepository = provider.getHistorikkRepository();
    }

    private BehandlingTema behandlingTemaFra(Behandling sisteBehandling) {
        final Optional<FamilieHendelseGrunnlagEntitet> grunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(sisteBehandling.getId());
        return BehandlingTema.fraFagsak(sisteBehandling.getFagsak(), grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
    }

    // Alle aktuelle enheter
    public List<OrganisasjonsEnhet> hentEnhetListe() {
        return enhetsTjeneste.hentEnhetListe();
    }

    // Brukes ved opprettelse av oppgaver før behandling har startet
    public OrganisasjonsEnhet finnBehandlendeEnhetFraSøker(Fagsak fagsak) {
        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(fagsak.getAktørId(), BehandlingTema.fraFagsak(fagsak, null));
        return sjekkMotKobletSak(fagsak, enhet);
    }

    // Brukes ved opprettelse av førstegangsbehandling
    public OrganisasjonsEnhet finnBehandlendeEnhetFraSøker(Behandling behandling) {
        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(behandling.getAktørId(), behandlingTemaFra(behandling));
        return sjekkMotKobletSak(behandling.getFagsak(), enhet);
    }

    private OrganisasjonsEnhet sjekkMotKobletSak(Fagsak sak, OrganisasjonsEnhet enhet) {
        FagsakRelasjon relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(sak).orElse(null);
        if (relasjon == null || !relasjon.getFagsakNrTo().isPresent()) {
            return enhet;
        }
        if (relasjon.getFagsakNrEn().getId().equals(sak.getId())) {
            Fagsak sak2 = relasjon.getFagsakNrTo().get();
            OrganisasjonsEnhet enhetFS2 = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(sak2.getAktørId(), BehandlingTema.fraFagsak(sak2, null));
            return enhetsTjeneste.enhetsPresedens(enhet, enhetFS2, true);
        } else {
            Fagsak sak1 = relasjon.getFagsakNrEn();
            OrganisasjonsEnhet enhetFS1 = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(sak1.getAktørId(), BehandlingTema.fraFagsak(sak1, null));
            return enhetsTjeneste.enhetsPresedens(enhetFS1, enhet, false);
        }
    }

    private Optional<OrganisasjonsEnhet> endretBehandlendeEnhetFraAndrePersoner(Behandling behandling, List<AktørId> aktører) {
        return enhetsTjeneste.oppdaterEnhetSjekkOppgitte(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetId(), aktører);
    }

    // Sjekk om andre angitte personer (Verge mm) har diskresjonskode som tilsier spesialenhet. Returnerer empty() hvis ingen endring.
    public Optional<OrganisasjonsEnhet> endretBehandlendeEnhetFraAndrePersoner(Behandling behandling, PersonIdent relatert) {
        AktørId aktørId = tpsTjeneste.hentAktørForFnr(relatert).orElse(null);
        if (aktørId == null) {
            return Optional.empty();
        }
        return enhetsTjeneste.oppdaterEnhetSjekkOppgitte(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetId(),
            Arrays.asList(aktørId));
    }

    // Sjekk om oppgitt annen part fra søknad har diskresjonskode som tilsier spesialenhet. Returnerer empty() hvis ingen endring.
    public Optional<OrganisasjonsEnhet> endretBehandlendeEnhetFraOppgittAnnenPart(Behandling behandling) {
        List<AktørId> annenPart = new ArrayList<>();
        finnAktørAnnenPart(behandling).ifPresent(annenPart::add);
        if (!annenPart.isEmpty()) {
            return endretBehandlendeEnhetFraAndrePersoner(behandling, annenPart);
        }
        return Optional.empty();
    }

    // Brukes for å sjekke om det er behov for å endre til spesialenheter når saken tas av vent.
    public Optional<OrganisasjonsEnhet> sjekkEnhetVedGjenopptak(Behandling behandling) {
        return sjekkEnhetForBehandlingMedEvtKobletSak(behandling);
    }

    // Brukes for å utlede enhet ved opprettelse av klage, revurdering, ny førstegangsbehandling, innsyn mv. Vil normalt videreføre enhet fra tidligere behandling
    public Optional<OrganisasjonsEnhet> sjekkEnhetVedNyAvledetBehandling(Fagsak fagsak) {
        Optional<Behandling> opprinneligBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (!opprinneligBehandling.isPresent()) {
            return Optional.of(finnBehandlendeEnhetFraSøker(fagsak));
        }
        OrganisasjonsEnhet enhet = sjekkEnhetForBehandlingMedEvtKobletSak(opprinneligBehandling.get())
            .orElse(opprinneligBehandling.get().getBehandlendeOrganisasjonsEnhet());
        return Optional.of(enhet);
    }

    // Brukes for å utlede enhet ved opprettelse av klage, revurdering, ny førstegangsbehandling, innsyn mv. Vil normalt videreføre enhet fra tidligere behandling
    public Optional<OrganisasjonsEnhet> sjekkEnhetVedNyAvledetBehandling(Behandling opprinneligBehandling) {
        return sjekkEnhetForBehandlingMedEvtKobletSak(opprinneligBehandling);
    }

    private Optional<OrganisasjonsEnhet> sjekkEnhetForBehandlingMedEvtKobletSak(Behandling behandling) {
        AktørId hovedPerson = behandling.getAktørId();
        Optional<AktørId> kobletPerson = Optional.empty();
        List<AktørId> relatertePersoner = new ArrayList<>();

        Optional<FagsakRelasjon> relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        if (relasjon.isPresent()) {
            if (relasjon.get().getFagsakNrEn().getId().equals(behandling.getFagsakId())) {
                kobletPerson = relasjon.get().getFagsakNrTo().map(Fagsak::getAktørId);
            } else {
                hovedPerson = relasjon.get().getFagsakNrEn().getAktørId();
                kobletPerson = Optional.of(behandling.getAktørId());
            }
        }

        finnAktørAnnenPart(behandling).ifPresent(relatertePersoner::add);

        return enhetsTjeneste.oppdaterEnhetSjekkRegistrerteRelasjoner(behandling.getBehandlendeOrganisasjonsEnhet().getEnhetId(), behandlingTemaFra(behandling),
            hovedPerson, kobletPerson, relatertePersoner);
    }

    private Optional<AktørId> finnAktørAnnenPart(Behandling behandling) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart).map(OppgittAnnenPartEntitet::getAktørId);
    }

    // Sjekk om enhet skal endres etter kobling av fagsak. Andre fagsak vil arve enhet fra første i relasjon, med mindre det er diskresjonskoder. empty() betyr ingen endring
    public Optional<OrganisasjonsEnhet> endretBehandlendeEnhetEtterFagsakKobling(Behandling behandling, FagsakRelasjon kobling) {
        Fagsak fagsak2 = kobling.getFagsakNrTo().orElse(null);
        Optional<OrganisasjonsEnhet> organisasjonsEnhet = Optional.empty();
        if (fagsak2 == null) {
            return organisasjonsEnhet;
        }

        if (behandling.getFagsakId().equals(kobling.getFagsakNrEn().getId())) {
            // Behandling = FS1 og enhet er styrende. Beholder enhet med mindre Fagsak 2 tilsier endring. Skal normalt sett ikke komme hit.
            OrganisasjonsEnhet enhetFS1 = behandling.getBehandlendeOrganisasjonsEnhet();
            OrganisasjonsEnhet enhetFS2 = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(fagsak2.getAktørId(), BehandlingTema.fraFagsak(fagsak2, null));
            OrganisasjonsEnhet presedens = enhetsTjeneste.enhetsPresedens(enhetFS1, enhetFS2, true);
            if (!presedens.getEnhetId().equals(enhetFS1.getEnhetId())) {
                organisasjonsEnhet = Optional.of(presedens);
            }
        } else {
            // Behandling = FS2 men bruker fra FS1 er styrende - med mindre vi tar presedens. Oppdatere Behandling ved behov.
            OrganisasjonsEnhet enhetFS1 = enhetsTjeneste.hentEnhetSjekkRegistrerteRelasjoner(kobling.getFagsakNrEn().getAktørId(),
                BehandlingTema.fraFagsak(kobling.getFagsakNrEn(), null));
            OrganisasjonsEnhet enhetFS2 = behandling.getBehandlendeOrganisasjonsEnhet();
            OrganisasjonsEnhet presedens = enhetsTjeneste.enhetsPresedens(enhetFS1, enhetFS2, false);
            if (!presedens.getEnhetId().equals(enhetFS2.getEnhetId())) {
                organisasjonsEnhet = Optional.of(presedens);
            }
        }
        return organisasjonsEnhet;
    }


    // Sjekk om angitt journalførende enhet er gyldig for enkelte oppgaver
    public boolean gyldigEnhetNfpNk(String enhetId) {
        return enhetsTjeneste.finnOrganisasjonsEnhet(enhetId).isPresent();
    }

    // Brukes for å sjekke om behandling skal flyttes etter endringer i NORG2-oppsett
    public Optional<OrganisasjonsEnhet> sjekkOppdatertEnhetEtterReallokering(Behandling behandling) {
        OrganisasjonsEnhet enhet = finnBehandlendeEnhetFraSøker(behandling);
        if (enhet.getEnhetId().equals(behandling.getBehandlendeEnhet())) {
            return Optional.empty();
        }
        return Optional.of(enhet);
    }

    // Returnerer enhetsnummer for NAV Klageinstans
    public OrganisasjonsEnhet getKlageInstans() {
        return enhetsTjeneste.getEnhetKlage();
    }

    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, HistorikkAktør endretAv, String begrunnelse) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        if (endretAv != null) {
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, nyEnhet, begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(nyEnhet);
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    private void lagHistorikkInnslagForByttBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, String begrunnelse, HistorikkAktør aktør) {
        OrganisasjonsEnhet eksisterende = behandling.getBehandlendeOrganisasjonsEnhet();
        String fraMessage = eksisterende != null ? eksisterende.getEnhetId() + " " + eksisterende.getEnhetNavn() : "ukjent";
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BYTT_ENHET)
            .medEndretFelt(HistorikkEndretFeltType.BEHANDLENDE_ENHET,
                fraMessage,
                nyEnhet.getEnhetId() + " " + nyEnhet.getEnhetNavn())
            .medBegrunnelse(begrunnelse);

        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(aktør);
        innslag.setType(HistorikkinnslagType.BYTT_ENHET);
        innslag.setBehandlingId(behandling.getId());
        builder.build(innslag);
        historikkRepository.lagre(innslag);
    }
}
