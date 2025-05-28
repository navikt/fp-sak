package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.UttakPerioderMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringUttakDto.class, adapter = Overstyringshåndterer.class)
public class UttakOverstyringshåndterer implements Overstyringshåndterer<OverstyringUttakDto> {

    private FastsettePerioderTjeneste tjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    private HistorikkinnslagRepository historikkinnslagRepository;

    UttakOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public UttakOverstyringshåndterer(FastsettePerioderTjeneste tjeneste,
                                      ForeldrepengerUttakTjeneste uttakTjeneste,
                                      UttakInputTjeneste uttakInputTjeneste,
                                      HistorikkinnslagRepository historikkinnslagRepository) {
        this.tjeneste = tjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringUttakDto dto, BehandlingReferanse ref) {
        var forrigeUttak = uttakTjeneste.hent(ref.behandlingId());
        var perioder = UttakPerioderMapper.map(dto.getPerioder(), forrigeUttak.getGjeldendePerioder());
        var uttakInput = uttakInputTjeneste.lagInput(ref.behandlingId());
        tjeneste.manueltFastsettePerioder(uttakInput, perioder);
        lagreHistorikkInnslag(dto, ref, forrigeUttak);
        return OppdateringResultat.utenOverhopp();
    }

    private void lagreHistorikkInnslag(OverstyringUttakDto dto, BehandlingReferanse ref, ForeldrepengerUttak forrigeUttak) {
        var historikkinnslag = UttakHistorikkUtil.forOverstyring()
            .lagHistorikkinnslag(ref, dto.getPerioder(), forrigeUttak.getGjeldendePerioder());
        historikkinnslag.ifPresent(innslag -> historikkinnslagRepository.lagre(innslag));
    }
}
