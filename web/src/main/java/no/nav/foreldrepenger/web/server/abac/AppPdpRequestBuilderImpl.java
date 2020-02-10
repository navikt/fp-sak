package no.nav.foreldrepenger.web.server.abac;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import no.nav.abac.common.xacml.CommonAttributter;
import no.nav.abac.foreldrepenger.xacml.ForeldrepengerAttributter;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumer;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;
import no.nav.vedtak.sikkerhet.abac.AbacAttributtSamling;
import no.nav.vedtak.sikkerhet.abac.PdpKlient;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;

@Dependent
@Alternative
@Priority(2)
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {
    public static final String ABAC_DOMAIN = "foreldrepenger";
    private static final MdcExtendedLogContext MDC_EXTENDED_LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$
    private PipRepository pipRepository;
    private AktørConsumerMedCache aktørConsumer;
    private JournalConsumer journalConsumer;

    public AppPdpRequestBuilderImpl() {
    }

    @Inject
    public AppPdpRequestBuilderImpl(PipRepository pipRepository, AktørConsumerMedCache aktørConsumer, JournalConsumer journalConsumer) {
        this.pipRepository = pipRepository;
        this.aktørConsumer = aktørConsumer;
        this.journalConsumer = journalConsumer;
    }

    private static void validerSamsvarBehandlingOgFagsak(Long behandlingId, Long fagsakId, Set<Long> fagsakIder) {
        List<Long> fagsakerSomIkkeErForventet = fagsakIder.stream()
            .filter(f -> !fagsakId.equals(f))
            .collect(Collectors.toList());
        if (!fagsakerSomIkkeErForventet.isEmpty()) {
            throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputManglerSamsvarBehandlingFagsak(behandlingId, fagsakerSomIkkeErForventet)
                .toException();
        }
    }

    @Override
    public PdpRequest lagPdpRequest(AbacAttributtSamling attributter) {
        Optional<Long> behandlingIder = utledBehandlingIder(attributter);
        Optional<PipBehandlingsData> behandlingData = behandlingIder.isPresent()
            ? pipRepository.hentDataForBehandling(behandlingIder.get())
            : Optional.empty();
        Set<Long> fagsakIder = behandlingData.isPresent()
            ? utledFagsakIder(attributter, behandlingData.get())
            : utledFagsakIder(attributter);

        behandlingData.ifPresent(pipBehandlingsData -> validerSamsvarBehandlingOgFagsak(behandlingIder.get(), pipBehandlingsData.getFagsakId(), fagsakIder));

        if (!fagsakIder.isEmpty()) {
            MDC_EXTENDED_LOG_CONTEXT.remove("fagsak");
            MDC_EXTENDED_LOG_CONTEXT.add("fagsak", fagsakIder.size() == 1 ? fagsakIder.iterator().next().toString() : fagsakIder.toString());
        }
        behandlingIder.ifPresent(behId -> {
            MDC_EXTENDED_LOG_CONTEXT.remove("behandling");
            MDC_EXTENDED_LOG_CONTEXT.add("behandling", behId);
        });

        Set<AktørId> aktørIder = utledAktørIder(attributter, fagsakIder);
        Set<String> aksjonspunktType = pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(attributter.getVerdier(AppAbacAttributtType.AKSJONSPUNKT_KODE));
        return behandlingData.isPresent()
            ? lagPdpRequest(attributter, aktørIder, aksjonspunktType, behandlingData.get())
            : lagPdpRequest(attributter, aktørIder, aksjonspunktType);
    }

    private PdpRequest lagPdpRequest(AbacAttributtSamling attributter, Set<AktørId> aktørIder, Collection<String> aksjonspunktType) {
        Set<String> aktører = aktørIder == null ? Collections.emptySet()
            : aktørIder.stream().map(AktørId::getId).collect(Collectors.toCollection(TreeSet::new));
        Set<String> fnrs = attributter.getVerdier(AppAbacAttributtType.FNR);

        PdpRequest pdpRequest = new PdpRequest();
        pdpRequest.put(CommonAttributter.RESOURCE_FELLES_DOMENE, ABAC_DOMAIN);
        pdpRequest.put(PdpKlient.ENVIRONMENT_AUTH_TOKEN, attributter.getIdToken());
        pdpRequest.put(CommonAttributter.XACML_1_0_ACTION_ACTION_ID, attributter.getActionType().getEksternKode());
        pdpRequest.put(CommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE, attributter.getResource().getEksternKode());
        if (!aktører.isEmpty()) {
            pdpRequest.put(CommonAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE, aktører);
        }
        if (!fnrs.isEmpty()) {
            pdpRequest.put(CommonAttributter.RESOURCE_FELLES_PERSON_FNR, fnrs);
        }
        pdpRequest.put(ForeldrepengerAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE, aksjonspunktType);
        return pdpRequest;
    }

    private PdpRequest lagPdpRequest(AbacAttributtSamling attributter, Set<AktørId> aktørIder, Collection<String> aksjonspunktType,
                                     PipBehandlingsData behandlingData) {
        PdpRequest pdpRequest = lagPdpRequest(attributter, aktørIder, aksjonspunktType);
        AbacUtil.oversettBehandlingStatus(behandlingData.getBehandligStatus())
            .ifPresent(it -> pdpRequest.put(ForeldrepengerAttributter.RESOURCE_FORELDREPENGER_SAK_BEHANDLINGSSTATUS, it.getEksternKode()));
        AbacUtil.oversettFagstatus(behandlingData.getFagsakStatus())
            .ifPresent(it -> pdpRequest.put(ForeldrepengerAttributter.RESOURCE_FORELDREPENGER_SAK_SAKSSTATUS, it.getEksternKode()));
        behandlingData.getAnsvarligSaksbehandler()
            .ifPresent(it -> pdpRequest.put(ForeldrepengerAttributter.RESOURCE_FORELDREPENGER_SAK_ANSVARLIG_SAKSBEHANDLER, it));
        return pdpRequest;
    }

    private Optional<Long> utledBehandlingIder(AbacAttributtSamling attributter) {
        Set<UUID> uuids = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_UUID);
        Set<Long> behandlingIdVerdier = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_ID);
        Set<Long> behandlingId0 = behandlingIdVerdier.stream().mapToLong(Long::valueOf).boxed().collect(Collectors.toSet());
        
        Set<Long> behandlingsIder = new LinkedHashSet<>(behandlingId0);
        behandlingsIder.addAll(pipRepository.behandlingsIdForUuid(uuids));
        behandlingsIder.addAll(pipRepository.behandlingsIdForOppgaveId(attributter.getVerdier(AppAbacAttributtType.OPPGAVE_ID)));

        if (behandlingsIder.isEmpty()) {
            return Optional.empty();
        } else if (behandlingsIder.size() == 1) {
            return Optional.of(behandlingsIder.iterator().next());
        }
        throw FeilFactory.create(PdpRequestBuilderFeil.class).ugyldigInputFlereBehandlingIder(behandlingsIder).toException();
    }

    private Set<Long> utledFagsakIder(AbacAttributtSamling attributter, PipBehandlingsData behandlingData) {
        Set<Long> fagsaker = utledFagsakIder(attributter);
        fagsaker.add(behandlingData.getFagsakId());
        return fagsaker;
    }

    private Set<Long> utledFagsakIder(AbacAttributtSamling attributter) {
        Set<Long> fagsakIder = new HashSet<>();
        fagsakIder.addAll(attributter.getVerdier(AppAbacAttributtType.FAGSAK_ID));
        
        // 
        fagsakIder.addAll(pipRepository.fagsakIderForSøker(tilAktørId(attributter.getVerdier(AppAbacAttributtType.SAKER_MED_FNR))));
        
        // fra saksnummer
        Set<String> saksnummere = attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER);
        fagsakIder.addAll(pipRepository.fagsakIdForSaksnummer(saksnummere));

        // journalpostIder
        Set<String> ikkePåkrevdJournalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.JOURNALPOST_ID);
        Set<JournalpostId> ikkePåkrevdeJournalpostId = ikkePåkrevdJournalpostIdVerdier.stream().map(JournalpostId::new).collect(Collectors.toSet());
        fagsakIder.addAll(pipRepository.fagsakIdForJournalpostId(ikkePåkrevdeJournalpostId));

        Set<String> påkrevdJournalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.EKSISTERENDE_JOURNALPOST_ID);
        Set<JournalpostId> påkrevdJournalpostId = påkrevdJournalpostIdVerdier.stream().map(JournalpostId::new).collect(Collectors.toSet());
        fagsakIder.addAll(hentOgSjekkAtFinnes(saksnummere, påkrevdJournalpostId));
        
        return fagsakIder;
    }

    private Set<Long> hentOgSjekkAtFinnes(Collection<String> saksnumre, Collection<JournalpostId> journalpostIder) {
        Set<Long> resultat = pipRepository.fagsakIdForJournalpostId(journalpostIder);
        if (resultat.size() == journalpostIder.size()) {
            return resultat;
        } else {
            validerJournalpostIdMotSaksnummer(saksnumre, journalpostIder);
            return Collections.emptySet();
        }
    }

    private Set<AktørId> utledAktørIder(AbacAttributtSamling attributter, Set<Long> fagsakIder) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);

        Set<AktørId> aktørIder = new HashSet<>();
        aktørIder.addAll(aktørIdVerdier.stream().map(AktørId::new).collect(Collectors.toSet()));
        aktørIder.addAll(pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIder));
        return aktørIder;
    }

    private Collection<AktørId> tilAktørId(Set<String> fnr) {
        if (fnr == null || fnr.isEmpty()) {
            return Collections.emptySet();
        }
        return aktørConsumer.hentAktørIdForPersonIdentSet(fnr).stream().map(id -> new AktørId(id)).collect(Collectors.toSet());
    }

    private Set<Long> validerJournalpostIdMotSaksnummer(Collection<String> saksnumre, Collection<JournalpostId> journalposter) {
        if (journalposter.isEmpty()) {
            return Collections.emptySet();
        }
        if (saksnumre.isEmpty()) {
            throw PdpRequestBuilderFeil.FACTORY.ugyldigInputPåkrevdJournalpostIdFinnesIkke(journalposter).toException();
        }

        HentKjerneJournalpostListeRequest hentKjerneJournalpostListeRequest = new HentKjerneJournalpostListeRequest();

        for (String saksnummer : saksnumre) {
            ArkivSak journalSak = new ArkivSak();
            journalSak.setArkivSakSystem(Fagsystem.GOSYS.getOffisiellKode());
            journalSak.setArkivSakId(saksnummer);
            hentKjerneJournalpostListeRequest.getArkivSakListe().add(journalSak);
        }

        try {
            HentKjerneJournalpostListeResponse hentKjerneJournalpostListeResponse = journalConsumer
                .hentKjerneJournalpostListe(hentKjerneJournalpostListeRequest);
            for (JournalpostId journalpost : journalposter) {
                Journalpost journalpost1 = hentKjerneJournalpostListeResponse.getJournalpostListe()
                    .stream().filter(jp -> journalpost.equals(new JournalpostId(jp.getJournalpostId()))).findFirst().orElse(null);
                if (journalpost1 == null) {
                    throw PdpRequestBuilderFeil.FACTORY.ugyldigInputPåkrevdJournalpostIdFinnesIkke(journalposter).toException();
                } else if (Journaltilstand.UTGAAR.equals(journalpost1.getJournaltilstand())) {
                    throw PdpRequestBuilderFeil.FACTORY.ugyldigInputJournalpostIdUtgått(journalpost.getVerdi()).toException();
                }
            }
            return Collections.emptySet();
        } catch (HentKjerneJournalpostListeSikkerhetsbegrensning | HentKjerneJournalpostListeUgyldigInput sikkerhetsbegrensning) { // NOSONAR
            throw PdpRequestBuilderFeil.FACTORY.ugyldigInputPåkrevdJournalpostIdFinnesIkke(journalposter).toException();
        }
    }

}
