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
    public AppRessursData lagAppRessursData(AbacDataAttributter dataAttributter) {
        var behandlingIder = utledBehandlingIder(dataAttributter);
        Optional<PipBehandlingsData> behandlingData = behandlingIder.isPresent()
            ? pipRepository.hentDataForBehandling(behandlingIder.get())
            : Optional.empty();
        var fagsakIder = behandlingData.map(behandlingsData -> utledFagsakIder(dataAttributter, behandlingsData))
            .orElseGet(() -> utledFagsakIder(dataAttributter));

        behandlingData.ifPresent(
            pipBehandlingsData -> validerSamsvarBehandlingOgFagsak(behandlingIder.get(), pipBehandlingsData.getFagsakId(), fagsakIder));

        if (!fagsakIder.isEmpty()) {
            var saksnumre = pipRepository.saksnummerForFagsakId(fagsakIder);
            MDC_EXTENDED_LOG_CONTEXT.remove("fagsak");
            MDC_EXTENDED_LOG_CONTEXT.add("fagsak", saksnumre.size() == 1 ? saksnumre.iterator().next() : saksnumre.toString());
        }
        behandlingIder.ifPresent(behId -> {
            MDC_EXTENDED_LOG_CONTEXT.remove("behandling");
            MDC_EXTENDED_LOG_CONTEXT.add("behandling", behId);
        });

        var auditAktørId = utledAuditAktørId(dataAttributter, fagsakIder);

        var aktørIder = utledAktørIder(dataAttributter, fagsakIder);
        var aksjonspunktTypeOverstyring = PipRepository.harAksjonspunktTypeOverstyring(dataAttributter.getVerdier(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON));

        Set<String> aktører = aktørIder.stream().map(AktørId::getId).collect(Collectors.toCollection(TreeSet::new));
        Set<String> fnrs = dataAttributter.getVerdier(AppAbacAttributtType.FNR);

        var builder = AppRessursData.builder()
            .medAuditAktørId(auditAktørId)
            .leggTilAktørIdSet(aktører)
            .leggTilFødselsnumre(fnrs);
        if (aksjonspunktTypeOverstyring) {
            builder.medOverstyring(PipOverstyring.OVERSTYRING);
        }
        behandlingData.map(PipBehandlingsData::getBehandligStatus).flatMap(AbacUtil::oversettBehandlingStatus)
            .ifPresent(builder::medBehandlingStatus);
        behandlingData.map(PipBehandlingsData::getFagsakStatus).flatMap(AbacUtil::oversettFagstatus)
            .ifPresent(builder::medFagsakStatus);
        behandlingData.flatMap(PipBehandlingsData::getAnsvarligSaksbehandler)
            .ifPresent(builder::medAnsvarligSaksbehandler);
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
        Set<Long> fagsakIder = new HashSet<>();
        fagsakIder.addAll(attributter.getVerdier(AppAbacAttributtType.FAGSAK_ID));

        //
        fagsakIder.addAll(pipRepository.fagsakIderForSøker(aktørIdStringTilAktørId(attributter.getVerdier(AppAbacAttributtType.SAKER_FOR_AKTØR))));

        // fra saksnummer
        Set<String> saksnummere = attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER);
        fagsakIder.addAll(pipRepository.fagsakIdForSaksnummer(saksnummere));

        // journalpostIder
        Set<String> ikkePåkrevdJournalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.JOURNALPOST_ID);
        var ikkePåkrevdeJournalpostId = ikkePåkrevdJournalpostIdVerdier.stream().map(JournalpostId::new).collect(Collectors.toSet());
        fagsakIder.addAll(pipRepository.fagsakIdForJournalpostId(ikkePåkrevdeJournalpostId));

        Set<String> påkrevdJournalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.EKSISTERENDE_JOURNALPOST_ID);
        var påkrevdJournalpostId = påkrevdJournalpostIdVerdier.stream().map(JournalpostId::new).collect(Collectors.toSet());
        fagsakIder.addAll(pipRepository.fagsakIdForJournalpostId(påkrevdJournalpostId));

        return fagsakIder;
    }

    private Set<AktørId> utledAktørIder(AbacDataAttributter attributter, Set<Long> fagsakIder) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);

        Set<AktørId> aktørIder = new HashSet<>();
        aktørIder.addAll(aktørIdVerdier.stream().map(AktørId::new).collect(Collectors.toSet()));
        aktørIder.addAll(pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIder));
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
