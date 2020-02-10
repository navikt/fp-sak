package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPerioder;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FastsetteUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakHistorikkUtil;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsetteUttakDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettUttakOppdaterer implements AksjonspunktOppdaterer<FastsetteUttakDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FastsettePerioderTjeneste tjeneste;
    private UttakRepository uttakRepository;
    private UttakInputTjeneste uttakInputTjeneste;

    FastsettUttakOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FastsettUttakOppdaterer(BehandlingRepositoryProvider repositoryProverider,
                                   HistorikkTjenesteAdapter historikkAdapter,
                                   FastsettePerioderTjeneste tjeneste,
                                   UttakInputTjeneste uttakInputTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.tjeneste = tjeneste;
        this.uttakRepository = repositoryProverider.getUttakRepository();
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsetteUttakDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();
        avbrytOverflødigOverstyrAksjonpunkt(behandling)
        .ifPresent(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));

        var input = uttakInputTjeneste.lagInput(behandling);
        UttakResultatEntitet forrigeResultat = håndterOverstyring(dto, input);
        lagHistorikkInnslag(behandling, dto, forrigeResultat);

        return resultatBuilder.build();
    }

    private UttakResultatEntitet håndterOverstyring(FastsetteUttakDto dto, UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().getBehandlingId();
        UttakResultatEntitet forrigeResultat = uttakRepository.hentUttakResultat(behandlingId);
        UttakResultatPerioder perioder = UttakPerioderMapper.map(dto.getPerioder(), forrigeResultat.getGjeldendePerioder());
        tjeneste.manueltFastsettePerioder(uttakInput, perioder);
        return forrigeResultat;
    }

    private void lagHistorikkInnslag(Behandling behandling, FastsetteUttakDto dto, UttakResultatEntitet forrigeResultat) {
        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forFastsetting().lagHistorikkinnslag(
            behandling, dto.getPerioder(), forrigeResultat.getGjeldendePerioder());
        historikkinnslag.forEach(innslag -> historikkAdapter.lagInnslag(innslag));
    }

    private Optional<Aksjonspunkt> avbrytOverflødigOverstyrAksjonpunkt(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);
    }
}
