package no.nav.foreldrepenger.web.server.abac;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;
import no.nav.vedtak.sikkerhet.abac.pdp.AppRessursData;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipBehandlingStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipFagsakStatus;
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

    private static void validerSamsvarBehandlingOgFagsak(PipBehandlingsData pipBehandlingsData, Optional<Saksnummer> saksnummer) {
        var fagsakerSomIkkeErForventet = saksnummer.filter(f -> !pipBehandlingsData.saksnummer().equals(f));
        if (fagsakerSomIkkeErForventet.isPresent()) {
            throw new ManglerTilgangException("FP-280301", String.format("Ugyldig input. Ikke samsvar mellom behandlingId %s og fagsakId %s",
                pipBehandlingsData.behandlingId(), fagsakerSomIkkeErForventet));
        }
    }

    @Override
    public AppRessursData lagAppRessursDataForSystembruker(AbacDataAttributter dataAttributter) {
        // Slår opp behandling/fagsak men ikke alle aktører i saken. Trenger kun behandlingstatus/fagsakstatus
        var behandlingUuid = utledBehandling(dataAttributter);
        var behandlingData = behandlingUuid.flatMap(pipRepository::hentDataForBehandlingUuid);
        var saksnummer = utledSaksnummer(dataAttributter, behandlingData.orElse(null));

        setLogContext(saksnummer, behandlingData);

        var builder = AppRessursData.builder();
        behandlingData.map(PipBehandlingsData::behandlingStatus).flatMap(AppPdpRequestBuilderImpl::oversettBehandlingStatus)
            .ifPresent(builder::medBehandlingStatus);
        behandlingData.map(PipBehandlingsData::fagsakStatus).flatMap(AppPdpRequestBuilderImpl::oversettFagstatus)
            .ifPresent(builder::medFagsakStatus);
        return builder.build();

    }

    @Override
    public AppRessursData lagAppRessursData(AbacDataAttributter dataAttributter) {
        var behandlingUuid = utledBehandling(dataAttributter);
        var behandlingData = behandlingUuid.flatMap(pipRepository::hentDataForBehandlingUuid);
        var saksnummer = utledSaksnummer(dataAttributter, behandlingData.orElse(null));

        behandlingData.ifPresent(d -> validerSamsvarBehandlingOgFagsak(d, saksnummer));

        setLogContext(saksnummer, behandlingData);

        var builder = AppRessursData.builder();

        Set<String> aktører = dataAttributter.getVerdier(AppAbacAttributtType.AKTØR_ID);
        Set<String> fnrs = dataAttributter.getVerdier(AppAbacAttributtType.FNR);

        builder.leggTilAktørIdSet(aktører)
            .leggTilFødselsnumre(fnrs);
        saksnummer.map(Saksnummer::getVerdi).ifPresent(builder::medSaksnummer);
        behandlingData.map(PipBehandlingsData::behandlingStatus).flatMap(AppPdpRequestBuilderImpl::oversettBehandlingStatus)
            .ifPresent(builder::medBehandlingStatus);
        behandlingData.map(PipBehandlingsData::fagsakStatus).flatMap(AppPdpRequestBuilderImpl::oversettFagstatus)
            .ifPresent(builder::medFagsakStatus);
        behandlingData.flatMap(PipBehandlingsData::getAnsvarligSaksbehandler)
            .ifPresent(builder::medAnsvarligSaksbehandler);
        var aksjonspunktTypeOverstyring = PipRepository.harAksjonspunktTypeOverstyring(dataAttributter.getVerdier(AppAbacAttributtType.AKSJONSPUNKT_DEFINISJON));
        if (aksjonspunktTypeOverstyring) {
            builder.medOverstyring(PipOverstyring.OVERSTYRING);
        }

        var auditAktørId = utledAuditAktørId(dataAttributter, saksnummer);
        builder.medAuditIdent(auditAktørId);
        return builder.build();

    }

    private static void setLogContext(Optional<Saksnummer> saksnummer, Optional<PipBehandlingsData> behandlingData) {
        saksnummer.ifPresent(s -> {
            MDC_EXTENDED_LOG_CONTEXT.remove("fagsak");
            MDC_EXTENDED_LOG_CONTEXT.add("fagsak", s.getVerdi());
        });
        behandlingData.ifPresent(bd -> {
            MDC_EXTENDED_LOG_CONTEXT.remove("behandling");
            MDC_EXTENDED_LOG_CONTEXT.add("behandling", bd.behandlingUuid());
            MDC_EXTENDED_LOG_CONTEXT.remove("behandlingId"); // Forenkler oppslag ved feilfinning
            MDC_EXTENDED_LOG_CONTEXT.add("behandlingId", bd.behandlingId());
        });
    }

    private Optional<UUID> utledBehandling(AbacDataAttributter attributter) {
        Set<UUID> uuids = attributter.getVerdier(AppAbacAttributtType.BEHANDLING_UUID);

        if (uuids.isEmpty()) {
            return Optional.empty();
        }
        if (uuids.size() == 1) {
            return uuids.stream().findFirst();
        }
        throw new TekniskException("FP-621834", String.format("Ugyldig input. Støtter bare 0 eller 1 behandling, men har %s", uuids));
    }

    private Optional<Saksnummer> utledSaksnummer(AbacDataAttributter attributter, PipBehandlingsData behandlingData) {
        Set<String> saksnummerString = attributter.getVerdier(AppAbacAttributtType.SAKSNUMMER);
        var saksnummer = new HashSet<>(saksnummerString.stream().map(Saksnummer::new).toList());
        Optional.ofNullable(behandlingData).map(PipBehandlingsData::saksnummer).ifPresent(saksnummer::add);

        // journalpostIder
        Set<String> journalpostIdVerdier = attributter.getVerdier(AppAbacAttributtType.JOURNALPOST_ID);
        if (!journalpostIdVerdier.isEmpty()) {
            saksnummer.addAll(pipRepository.saksnummerForJournalpostId(journalpostIdVerdier));
        }
        if (saksnummer.isEmpty()) {
            return Optional.empty();
        }
        if (saksnummer.size() == 1) {
            return saksnummer.stream().findFirst();
        }
        throw new TekniskException("FP-621834", String.format("Ugyldig input. Støtter bare 0 eller 1 sak, men har %s", saksnummer));
    }

    private String utledAuditAktørId(AbacDataAttributter attributter, Optional<Saksnummer> saksnummer) {
        Set<String> aktørIdVerdier = attributter.getVerdier(AppAbacAttributtType.AKTØR_ID);
        Set<String> personIdentVerdier = attributter.getVerdier(AppAbacAttributtType.FNR);

        return saksnummer.flatMap(pipRepository::hentAktørIdSomEierFagsak).map(AktørId::getId)
            .or(() -> aktørIdVerdier.stream().findFirst())
            .or(() -> personIdentVerdier.stream().findFirst())
            .orElse(null);
    }

    public static Optional<PipFagsakStatus> oversettFagstatus(FagsakStatus fagsakStatus) {
        return switch (fagsakStatus) {
            case OPPRETTET -> Optional.of(PipFagsakStatus.OPPRETTET);
            case UNDER_BEHANDLING -> Optional.of(PipFagsakStatus.UNDER_BEHANDLING);
            case null, default -> Optional.empty();
        };
    }

    public static Optional<PipBehandlingStatus> oversettBehandlingStatus(BehandlingStatus behandlingStatus) {
        return switch (behandlingStatus) {
            case OPPRETTET -> Optional.of(PipBehandlingStatus.OPPRETTET);
            case UTREDES -> Optional.of(PipBehandlingStatus.UTREDES);
            case FATTER_VEDTAK -> Optional.of(PipBehandlingStatus.FATTE_VEDTAK);
            case null, default -> Optional.empty();
        };
    }

}
