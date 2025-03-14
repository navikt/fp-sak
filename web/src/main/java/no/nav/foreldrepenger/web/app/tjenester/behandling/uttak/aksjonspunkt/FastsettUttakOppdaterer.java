package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FastsetteUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.UttakHistorikkUtil;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsetteUttakDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettUttakOppdaterer implements AksjonspunktOppdaterer<FastsetteUttakDto> {

    private FastsettePerioderTjeneste tjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public FastsettUttakOppdaterer(FastsettePerioderTjeneste tjeneste,
                                   ForeldrepengerUttakTjeneste uttakTjeneste,
                                   UttakInputTjeneste uttakInputTjeneste,
                                   BehandlingRepository behandlingRepository,
                                   HistorikkinnslagRepository historikkinnslagRepository) {
        this.tjeneste = tjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    FastsettUttakOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(FastsetteUttakDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getRef();
        var resultatBuilder = OppdateringResultat.utenTransisjon();
        avbrytOverflødigOverstyrAksjonpunkt(behandling)
            .ifPresent(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));

        var input = uttakInputTjeneste.lagInput(behandling.behandlingId());
        var forrigeResultat = håndterOverstyring(dto, input);
        lagHistorikkInnslag(behandling, dto, forrigeResultat);

        return resultatBuilder.build();
    }

    private ForeldrepengerUttak håndterOverstyring(FastsetteUttakDto dto, UttakInput uttakInput) {
        var behandlingId = uttakInput.getBehandlingReferanse().behandlingId();
        var forrigeResultat = uttakTjeneste.hent(behandlingId);
        var perioder = UttakPerioderMapper.map(dto.getPerioder(), forrigeResultat.getGjeldendePerioder());
        tjeneste.manueltFastsettePerioder(uttakInput, perioder);
        return forrigeResultat;
    }

    private void lagHistorikkInnslag(BehandlingReferanse behandling, FastsetteUttakDto dto, ForeldrepengerUttak forrigeResultat) {
        var historikkinnslag = UttakHistorikkUtil.forFastsetting().lagHistorikkinnslag(
            behandling, dto.getPerioder(), forrigeResultat.getGjeldendePerioder());
        historikkinnslag.ifPresent(innslag -> historikkinnslagRepository.lagre(innslag));
    }

    private Optional<Aksjonspunkt> avbrytOverflødigOverstyrAksjonpunkt(BehandlingReferanse referanse) {
        var behandling = behandlingRepository.hentBehandling(referanse.behandlingId());
        return behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);
    }
}
