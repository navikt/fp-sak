package no.nav.foreldrepenger.web.server.abac;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;
import no.nav.vedtak.sikkerhet.abac.pdp.AppRessursData;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipOverstyring;

@Dependent
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {

    private static final MdcExtendedLogContext MDC_EXTENDED_LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");
    private PipRepository pipRepository;

    public AppPdpRequestBuilderImpl() {
    }

    @Inject
    public AppPdpRequestBuilderImpl(PipRepository pipRepository) {
        this.pipRepository = pipRepository;
    }

    private static void validerSamsvarBehandlingOgFagsak(Long behandlingId, Long fagsakId, Set<Long> fagsakIder) {
        var fagsakerSomIkkeErForventet = fagsakIder.stream()
                .filter(f -> !fagsakId.equals(f))
                .toList();
        if (!fagsakerSomIkkeErForventet.isEmpty()) {
            throw new ManglerTilgangException("FP-280301", String.format("Ugyldig input. Ikke samsvar mellom behandlingId %s og fagsakId %s", behandlingId, fagsakerSomIkkeErForventet));
        }
    }

    @Override
    public AppRessursData lagAppRessursDataForSystembruker(AbacDataAttributter dataAttributter) {
        // Slår opp behandling/fagsak men ikke alle aktører i saken. Trenger kun behandlingstatus/fagsakstatus
        return lagAppressursDataInternt(dataAttributter, true);
    }

    @Override
    public AppRessursData lagAppRessursData(AbacDataAttributter dataAttributter) {
        return lagAppressursDataInternt(dataAttributter, false);
    }

    public AppRessursData lagAppressursDataInternt(AbacDataAttributter dataAttributter, boolean systembruker) {
        var behandlingIder = utledBehandlingIder(dataAttributter);
        var behandlingData = behandlingIder.flatMap(pipRepository::hentDataForBehandling);
        var fagsakIder = behandlingData.map(behandlingsData -> utledFagsakIder(dataAttributter, behandlingsData))
            .orElseGet(() -> utledFagsakIder(dataAttributter));

        behandlingData.ifPresent(d -> validerSamsvarBehandlingOgFagsak(behandlingIder.get(), d.getFagsakId(), fagsakIder));

        if (!fagsakIder.isEmpty()) {
            var saksnumre = pipRepository.saksnummerForFagsakId(fagsakIder);
            MDC_EXTENDED_LOG_CONTEXT.remove("fagsak");
            MDC_EXTENDED_LOG_CONTEXT.add("fagsak", saksnumre.size() == 1 ? saksnumre.iterator().next() : saksnumre.toString());
        }
        behandlingIder.ifPresent(behId -> {
            MDC_EXTENDED_LOG_CONTEXT.remove("behandling");
            MDC_EXTENDED_LOG_CONTEXT.add("behandling", behId);
        });

        var builder = AppRessursData.builder();

        if (!systembruker) {
            var auditAktørId = utledAuditAktørId(dataAttributter, fagsakIder);

            var aktørIder = utledAktørIder(dataAttributter, fagsakIder);

            Set<String> aktører = aktørIder.stream().map(AktørId::getId).collect(Collectors.toCollection(TreeSet::new));
            Set<String> fnrs = dataAttributter.getVerdier(AppAbacAttributtType.FNR);

            builder
                .medAuditIdent(auditAktørId)
                .leggTilAktørIdSet(aktører)
                .leggTilFødselsnumre(fnrs);
            var aksjonspunktTypeOverstyring = PipRepository.harAksjonspunktTypeOverstyring(dataAttributter.getVerdier(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON));
            if (aksjonspunktTypeOverstyring) {
                builder.medOverstyring(PipOverstyring.OVERSTYRING);
            }
            behandlingData.flatMap(PipBehandlingsData::getAnsvarligSaksbehandler)
                .ifPresent(builder::medAnsvarligSaksbehandler);
        }

        behandlingData.map(PipBehandlingsData::getBehandligStatus).flatMap(AbacUtil::oversettBehandlingStatus)
            .ifPresent(builder::medBehandlingStatus);
        behandlingData.map(PipBehandlingsData::getFagsakStatus).flatMap(AbacUtil::oversettFagstatus)
            .ifPresent(builder::medFagsakStatus);
        return builder.build();
    }

    private Optional<Long> utledBehandlingIder(AbacDataAttributter attributter) {
        Set<UUID> uuids = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_UUID);
        Set<Long> behandlingIdVerdier = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_ID);
        var behandlingId0 = behandlingIdVerdier.stream().mapToLong(Long::valueOf).boxed().toList();

        Set<Long> behandlingsIder = new LinkedHashSet<>(behandlingId0);
        behandlingsIder.addAll(pipRepository.behandlingsIdForUuid(uuids));

        if (behandlingsIder.isEmpty()) {
            return Optional.empty();
        }
        if (behandlingsIder.size() == 1) {
            return Optional.of(behandlingsIder.iterator().next());
        }
        throw new TekniskException("FP-621834", String.format("Ugyldig input. Støtter bare 0 eller 1 behandling, men har %s", behandlingsIder));
    }

    private Set<Long> utledFagsakIder(AbacDataAttributter attributter, PipBehandlingsData behandlingData) {
        var fagsaker = utledFagsakIder(attributter);
        fagsaker.add(behandlingData.getFagsakId());
        return fagsaker;
    }

    private Set<Long> utledFagsakIder(AbacDataAttributter attributter) {
        Set<Long> fagsakIder = new HashSet<>(attributter.getVerdier(AppAbacAttributtType.FAGSAK_ID));

        // Søk - sjekker alle saker for bruker ved innkommende søk - burde heller filtrere resultatet når det blir tilgjengelig
        Set<String> aktørIdFraSøk = attributter.getVerdier(AppAbacAttributtType.SAKER_FOR_AKTØR);
        if (!aktørIdFraSøk.isEmpty()) {
            fagsakIder.addAll(pipRepository.fagsakIderForSøker(aktørIdStringTilAktørId(aktørIdFraSøk)));
        }

        // fra saksnummer
        Set<String> saksnummere = attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER);
        if (!saksnummere.isEmpty()) {
            fagsakIder.addAll(pipRepository.fagsakIdForSaksnummer(saksnummere));
        }

        // journalpostIder
        Set<String> ikkePåkrevdJournalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.JOURNALPOST_ID);
        var ikkePåkrevdeJournalpostId = ikkePåkrevdJournalpostIdVerdier.stream().map(JournalpostId::new).collect(Collectors.toSet());
        if (!ikkePåkrevdeJournalpostId.isEmpty()) {
            fagsakIder.addAll(pipRepository.fagsakIdForJournalpostId(ikkePåkrevdeJournalpostId));
        }

        Set<String> påkrevdJournalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.EKSISTERENDE_JOURNALPOST_ID);
        var påkrevdJournalpostId = påkrevdJournalpostIdVerdier.stream().map(JournalpostId::new).collect(Collectors.toSet());
        if (!påkrevdJournalpostId.isEmpty()) {
            fagsakIder.addAll(pipRepository.fagsakIdForJournalpostId(påkrevdJournalpostId));
        }

        return fagsakIder;
    }

    private Set<AktørId> utledAktørIder(AbacDataAttributter attributter, Set<Long> fagsakIder) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);

        var aktørIder = new HashSet<>(aktørIdVerdier.stream().map(AktørId::new).collect(Collectors.toSet()));
        if (!fagsakIder.isEmpty()) {
            aktørIder.addAll(pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIder));
        }
        return aktørIder;
    }

    private String utledAuditAktørId(AbacDataAttributter attributter, Set<Long> fagsakIder) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);
        Set<String> personIdentVerdier = attributter.getVerdier(AppAbacAttributtType.FNR);

        return pipRepository.hentAktørIdSomEierFagsaker(fagsakIder).stream().findFirst().map(AktørId::getId)
            .or(() -> aktørIdVerdier.stream().findFirst())
            .or(() -> personIdentVerdier.stream().findFirst())
            .orElse(null);
    }

    private Collection<AktørId> aktørIdStringTilAktørId(Set<String> aktørId) {
        if (aktørId == null || aktørId.isEmpty()) {
            return Collections.emptySet();
        }
        return aktørId.stream().map(AktørId::new).collect(Collectors.toSet());
    }

}
