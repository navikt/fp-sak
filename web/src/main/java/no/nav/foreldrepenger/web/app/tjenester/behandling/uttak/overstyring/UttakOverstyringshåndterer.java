package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.UttakPerioderMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;

//Requestscoped pga at vi må mellomlagre forrige uttaksresultat i field for å bruke til historikk
@RequestScoped
@DtoTilServiceAdapter(dto = OverstyringUttakDto.class, adapter = Overstyringshåndterer.class)
public class UttakOverstyringshåndterer implements Overstyringshåndterer<OverstyringUttakDto> {

    private FastsettePerioderTjeneste tjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    private ForeldrepengerUttak forrigeUttak;
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
    public OppdateringResultat håndterOverstyring(OverstyringUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        this.forrigeUttak = uttakTjeneste.hent(kontekst.getBehandlingId());
        var perioder = UttakPerioderMapper.map(dto.getPerioder(), forrigeUttak.getGjeldendePerioder());
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        tjeneste.manueltFastsettePerioder(uttakInput, perioder);
        return OppdateringResultat.utenOverhopp();
    }

    @Override
    public void lagHistorikkInnslag(OverstyringUttakDto dto, Behandling behandling) {
        var historikkinnslag = UttakHistorikkUtil.forOverstyring()
            .lagHistorikkinnslag(BehandlingReferanse.fra(behandling), dto.getPerioder(), forrigeUttak.getGjeldendePerioder());
        historikkinnslag.ifPresent(innslag -> historikkinnslagRepository.lagre(innslag));
    }
}
