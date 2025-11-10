package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.kontrakter.formidling.v3.BrevmalDto;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOperasjonerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto.KontrollresultatDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto.TotrinnskontrollSkjermlenkeContextDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårDto;

public record FagsakBehandlingDto(@NotNull UUID uuid,
                                  @NotNull BehandlingOperasjonerDto behandlingTillatteOperasjoner,
                                  @NotNull List<BrevmalDto> brevmaler,
                                  @NotNull List<TotrinnskontrollSkjermlenkeContextDto> totrinnskontrollÅrsaker,
                                  AksjonspunktDto risikoAksjonspunkt,
                                  KontrollresultatDto kontrollResultat,
                                  @NotNull boolean ugunstAksjonspunkt,
                                  String ansvarligSaksbehandler,
                                  LocalDateTime avsluttet,
                                  BehandlingÅrsakDto førsteÅrsak,
                                  @NotNull boolean gjeldendeVedtak,
                                  @NotNull LocalDateTime opprettet,
                                  @NotNull boolean toTrinnsBehandling,
                                  @NotNull Long versjon,
                                  @NotNull BehandlingType type,
                                  @NotNull BehandlingStatus status,
                                  @NotNull String behandlendeEnhetId,
                                  @NotNull boolean aktivPapirsøknad,
                                  @NotNull String behandlendeEnhetNavn,
                                  @NotNull boolean behandlingHenlagt,
                                  @NotNull Språkkode språkkode,
                                  @NotNull boolean behandlingPåVent,
                                  BehandlingsresultatDto behandlingsresultat,
                                  @NotNull List<BehandlingÅrsakDto> behandlingÅrsaker,
                                  @NotNull List<VilkårDto> vilkår,
                                  String fristBehandlingPåVent,
                                  @NotNull List<ResourceLink> links) {
}
