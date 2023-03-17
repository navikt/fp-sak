package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event.BehandlingEnhetEventPubliserer;

@ApplicationScoped
public class BehandlendeEnhetTjeneste {

    private EnhetsTjeneste enhetsTjeneste;
    private BehandlingEnhetEventPubliserer eventPubliserer;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private HistorikkRepository historikkRepository;

    public BehandlendeEnhetTjeneste() {
        // For CDI
    }

    @Inject
    public BehandlendeEnhetTjeneste(EnhetsTjeneste enhetsTjeneste,
                                    BehandlingEnhetEventPubliserer eventPubliserer,
                                    BehandlingRepositoryProvider provider) {
        this.enhetsTjeneste = enhetsTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.personopplysningRepository = provider.getPersonopplysningRepository();
        this.fagsakRelasjonRepository = provider.getFagsakRelasjonRepository();
        this.fagsakRepository = provider.getFagsakRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.historikkRepository = provider.getHistorikkRepository();
    }

    // Alle aktuelle enheter
    public List<OrganisasjonsEnhet> hentEnhetListe() {
        return enhetsTjeneste.hentEnhetListe();
    }

    // Brukes ved opprettelse av alle typer behandlinger og oppgaver
    public OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak) {
        var enhet = finnEnhetFor(fagsak);
        return sjekkMotKobletSak(fagsak, enhet);
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetForUkoblet(Fagsak fagsak, OrganisasjonsEnhet sisteBrukt) {
        if (gyldigEnhetNfpNk(sisteBrukt.enhetId())) return sisteBrukt;
        return enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), BehandlingTema.fraFagsak(fagsak, null));
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetForFagsakId(Long fagsakId) {
        return finnEnhetFor(fagsakRepository.finnEksaktFagsak(fagsakId));
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetForAktørId(AktørId aktørId) {
        return enhetsTjeneste.hentEnhetSjekkKunAktør(aktørId, BehandlingTema.FORELDREPENGER);
    }

    private OrganisasjonsEnhet finnEnhetFor(Fagsak fagsak) {
        var forrigeEnhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .filter(b -> gyldigEnhetNfpNk(b.getBehandlendeEnhet()))
            .map(Behandling::getBehandlendeOrganisasjonsEnhet);
        return forrigeEnhet.orElse(enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), BehandlingTema.fraFagsak(fagsak, null)));
    }

    // Brukes for å sjekke om det er behov for å flytte eller endre til spesialenheter når saken tas av vent.
    public Optional<OrganisasjonsEnhet> sjekkEnhetEtterEndring(Behandling behandling) {
        var enhet = behandling.getBehandlendeOrganisasjonsEnhet();
        if (enhet.equals(EnhetsTjeneste.getEnhetKlage())) {
            return Optional.empty();
        }
        var oppdatertEnhet = getOrganisasjonsEnhetEtterEndring(behandling, enhet).orElse(enhet);
        var enhetFraKobling = sjekkMotKobletSak(behandling.getFagsak(), oppdatertEnhet);
        return enhet.equals(enhetFraKobling) ? Optional.empty() : Optional.of(enhetFraKobling);
    }

    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Behandling behandling, OrganisasjonsEnhet enhet) {
        var hovedPerson = behandling.getAktørId();
        Set<AktørId> allePersoner = new HashSet<>();

        finnAktørAnnenPart(behandling).ifPresent(allePersoner::add);

        allePersoner.addAll(finnAktørIdFraPersonopplysninger(behandling));

        return getOrganisasjonsEnhetEtterEndring(behandling.getFagsak(), enhet, hovedPerson, allePersoner);
    }

    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Fagsak fagsak, OrganisasjonsEnhet enhet, AktørId hovedPerson, Set<AktørId> allePersoner) {
        allePersoner.add(hovedPerson);

        var relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
        relasjon.map(FagsakRelasjon::getFagsakNrEn).map(Fagsak::getAktørId).ifPresent(allePersoner::add);
        relasjon.flatMap(FagsakRelasjon::getFagsakNrTo).map(Fagsak::getAktørId).ifPresent(allePersoner::add);

        return enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(),
            BehandlingTema.fraFagsak(fagsak, null), hovedPerson, allePersoner);
    }


    private Optional<AktørId> finnAktørAnnenPart(Behandling behandling) {
        return personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getId()).map(OppgittAnnenPartEntitet::getAktørId);
    }

    private Set<AktørId> finnAktørIdFraPersonopplysninger(Behandling behandling) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .map(PersonopplysningEntitet::getAktørId)
            .collect(Collectors.toSet());
    }

    // Sjekk om enhet skal endres etter kobling av fagsak. Andre fagsak vil arve enhet fra første i relasjon, med mindre det er diskresjonskoder. empty() betyr ingen endring
    public Optional<OrganisasjonsEnhet> endretBehandlendeEnhetEtterFagsakKobling(Behandling behandling, FagsakRelasjon kobling) {

        var eksisterendeEnhet = behandling.getBehandlendeOrganisasjonsEnhet();
        var nyEnhet = sjekkMotKobletSak(behandling.getFagsak(), eksisterendeEnhet);

        return eksisterendeEnhet.equals(nyEnhet) ? Optional.empty() : Optional.of(nyEnhet);
    }

    private OrganisasjonsEnhet sjekkMotKobletSak(Fagsak sak, OrganisasjonsEnhet enhet) {
        var relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(sak).orElse(null);
        if (relasjon == null || relasjon.getFagsakNrTo().isEmpty()) {
            return enhet;
        }
        var relatertSak = relasjon.getRelatertFagsak(sak).get();  // NOSONAR sjekket over
        var relatertEnhet = finnEnhetFor(relatertSak);
        if (sak.getOpprettetTidspunkt().isBefore(relatertSak.getOpprettetTidspunkt())) {
            return enhetsTjeneste.enhetsPresedens(enhet, relatertEnhet);
        }
        return enhetsTjeneste.enhetsPresedens(relatertEnhet, enhet);
    }

    // Sjekk om angitt journalførende enhet er gyldig for enkelte oppgaver
    public boolean gyldigEnhetNfpNk(String enhetId) {
        return enhetsTjeneste.finnOrganisasjonsEnhet(enhetId).isPresent();
    }

    // Brukes for å sjekke om behandling skal flyttes etter endringer i NORG2-oppsett
    public Optional<OrganisasjonsEnhet> sjekkOppdatertEnhetEtterReallokering(Behandling behandling) {
        var enhet = finnBehandlendeEnhetFor(behandling.getFagsak());
        if (enhet.enhetId().equals(behandling.getBehandlendeEnhet())) {
            return Optional.empty();
        }
        return Optional.of(getOrganisasjonsEnhetEtterEndring(behandling.getFagsak(), enhet, behandling.getAktørId(), new HashSet<>()).orElse(enhet));
    }

    // Returnerer enhetsnummer for NAV Klageinstans
    public static OrganisasjonsEnhet getKlageInstans() {
        return EnhetsTjeneste.getEnhetKlage();
    }

    public static boolean erUtlandsEnhet(Behandling behandling) {
        return EnhetsTjeneste.getEnhetUtland().enhetId().equals(behandling.getBehandlendeEnhet());
    }


    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhetUtland(Behandling behandling, HistorikkAktør endretAv, String begrunnelse) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        if (erUtlandsEnhet(behandling)) {
            return;
        }
        if (endretAv != null) {
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, EnhetsTjeneste.getEnhetUtland(), begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(EnhetsTjeneste.getEnhetUtland());
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, HistorikkAktør endretAv, String begrunnelse) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        if (endretAv != null) {
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, nyEnhet, begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(nyEnhet);
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    private void lagHistorikkInnslagForByttBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, String begrunnelse, HistorikkAktør aktør) {
        var eksisterende = behandling.getBehandlendeOrganisasjonsEnhet();
        var fraMessage = eksisterende != null ? eksisterende.enhetId() + " " + eksisterende.enhetNavn() : "ukjent";
        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BYTT_ENHET)
            .medEndretFelt(HistorikkEndretFeltType.BEHANDLENDE_ENHET,
                fraMessage,
                nyEnhet.enhetId() + " " + nyEnhet.enhetNavn())
            .medBegrunnelse(begrunnelse);

        var innslag = new Historikkinnslag();
        innslag.setAktør(aktør);
        innslag.setType(HistorikkinnslagType.BYTT_ENHET);
        innslag.setBehandlingId(behandling.getId());
        builder.build(innslag);
        historikkRepository.lagre(innslag);
    }
}
