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
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.mdc.LoggFelter;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;
import no.nav.vedtak.sikkerhet.abac.pdp.AppRessursData;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipBehandlingStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipFagsakStatus;
import no.nav.vedtak.sikkerhet.abac.pipdata.PipOverstyring;

@Dependent
public class AppPdpRequestBuilderImpl implements PdpRequestBuilder {

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

        var builder = AppRessursData.builder();
        behandlingData.map(PipBehandlingsData::behandlingStatus).flatMap(AppPdpRequestBuilderImpl::oversettBehandlingStatus)
            .ifPresent(builder::medBehandlingStatus);
        behandlingData.map(PipBehandlingsData::fagsakStatus).flatMap(AppPdpRequestBuilderImpl::oversettFagstatus)
            .ifPresent(builder::medFagsakStatus);
        // Logging
        saksnummer.ifPresent(s -> builder.medLoggSaksnummer(s.getVerdi()));
        behandlingData.ifPresent(d -> builder.medLoggBehandling(d.behandlingUuid()));
        behandlingData.ifPresent(d -> builder.medLoggFelt(LoggFelter.BEHANDLING_ID, d.behandlingId().toString()));
        return builder.build();

    }

    @Override
    public AppRessursData lagAppRessursData(AbacDataAttributter dataAttributter) {
        var behandlingUuid = utledBehandling(dataAttributter);
        var behandlingData = behandlingUuid.flatMap(pipRepository::hentDataForBehandlingUuid);
        var saksnummer = utledSaksnummer(dataAttributter, behandlingData.orElse(null));

        behandlingData.ifPresent(d -> validerSamsvarBehandlingOgFagsak(d, saksnummer));

        var builder = AppRessursData.builder()
            .leggTilIdenter(dataAttributter.getVerdier(AppAbacAttributtType.AKTØR_ID))
            .leggTilIdenter(dataAttributter.getVerdier(AppAbacAttributtType.FNR));
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
        // Logging
        saksnummer.ifPresent(s -> builder.medLoggSaksnummer(s.getVerdi()));
        behandlingData.ifPresent(d -> builder.medLoggBehandling(d.behandlingUuid()));
        behandlingData.ifPresent(d -> builder.medLoggFelt(LoggFelter.BEHANDLING_ID, d.behandlingId().toString()));

        return builder.build();

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
