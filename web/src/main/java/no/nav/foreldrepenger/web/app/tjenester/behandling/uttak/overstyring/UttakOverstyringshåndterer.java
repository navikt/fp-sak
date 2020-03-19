package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.UttakResultatPerioder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.UttakPerioderMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringUttakDto;

//Requestscoped pga at vi må mellomlagre forrige uttaksresultat i field for å bruke til historikk
@RequestScoped
@DtoTilServiceAdapter(dto = OverstyringUttakDto.class, adapter = Overstyringshåndterer.class)
public class UttakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringUttakDto> {

    private FastsettePerioderTjeneste tjeneste;
    private UttakRepository uttakRepository;
    private UttakResultatEntitet forrigeResultat;
    private UttakInputTjeneste uttakInputTjeneste;

    UttakOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public UttakOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                      HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                      FastsettePerioderTjeneste tjeneste,
                                      UttakInputTjeneste uttakInputTjeneste) {
        super(historikkTjenesteAdapter, OVERSTYRING_AV_UTTAKPERIODER);
        this.tjeneste = tjeneste;
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        this.forrigeResultat = uttakRepository.hentUttakResultat(kontekst.getBehandlingId());
        UttakResultatPerioder perioder = UttakPerioderMapper.map(dto.getPerioder(), forrigeResultat.getGjeldendePerioder());
        var uttakInput = uttakInputTjeneste.lagInput(kontekst.getBehandlingId());
        tjeneste.manueltFastsettePerioder(uttakInput, perioder);
        return OppdateringResultat.utenOveropp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringUttakDto dto) {
        List<Historikkinnslag> historikkinnslag = UttakHistorikkUtil.forOverstyring().lagHistorikkinnslag(
            behandling, dto.getPerioder(), forrigeResultat.getGjeldendePerioder());
        historikkinnslag.forEach(innslag -> getHistorikkAdapter().lagInnslag(innslag));
    }
}
