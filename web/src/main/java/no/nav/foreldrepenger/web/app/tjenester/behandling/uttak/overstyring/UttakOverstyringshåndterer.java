package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.UttakPerioderMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;

//Requestscoped pga at vi må mellomlagre forrige uttaksresultat i field for å bruke til historikk
@RequestScoped
@DtoTilServiceAdapter(dto = OverstyringUttakDto.class, adapter = Overstyringshåndterer.class)
public class UttakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringUttakDto> {

    private FastsettePerioderTjeneste tjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    private ForeldrepengerUttak forrigeUttak;
    private Historikkinnslag2Repository historikkinnslag2Repository;

    UttakOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public UttakOverstyringshåndterer(FastsettePerioderTjeneste tjeneste,
                                      ForeldrepengerUttakTjeneste uttakTjeneste,
                                      UttakInputTjeneste uttakInputTjeneste,
                                      Historikkinnslag2Repository historikkinnslag2Repository) {
        super(OVERSTYRING_AV_UTTAKPERIODER);
        this.tjeneste = tjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
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
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringUttakDto dto) {
        var historikkinnslag = UttakHistorikkUtil.forOverstyring()
            .lagHistorikkinnslag(BehandlingReferanse.fra(behandling), dto.getPerioder(), forrigeUttak.getGjeldendePerioder());
        historikkinnslag.forEach(innslag -> historikkinnslag2Repository.lagre(innslag));
    }
}
