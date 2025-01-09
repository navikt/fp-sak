package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderSøknadsfristDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderSøknadsfristOppdaterer implements AksjonspunktOppdaterer<VurderSøknadsfristDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;
    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    VurderSøknadsfristOppdaterer() {
    }

    @Inject
    public VurderSøknadsfristOppdaterer(HistorikkinnslagRepository historikkinnslagRepository, BehandlingRepositoryProvider repositoryProvider) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    @Override
    public OppdateringResultat oppdater(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var søknad = søknadRepository.hentSøknad(behandlingId);

        var historikkinnslag = lagHistorikkinnslag(dto, param, søknad);
        historikkinnslagRepository.lagre(historikkinnslag);
        lagreResultat(behandlingId, dto, søknad);
        return OppdateringResultat.utenOverhopp();
    }

    private Historikkinnslag lagHistorikkinnslag(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param, SøknadEntitet søknad) {
        var builder = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getFagsakId())
            .medTittel(SkjermlenkeType.SOEKNADSFRIST)
            .addLinje(fraTilEquals("Søknadsfrist", null,  dto.harGyldigGrunn()
                ? "Gyldig grunn til at søknaden er satt frem for sent"
                : "Ingen gyldig grunn til at søknaden er satt frem for sent"));

        if (dto.harGyldigGrunn()) {
            var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(param.getBehandlingId());
            var lagretMottattDato = uttaksperiodegrense.getMottattDato();
            var tidligereAnseesMottattDato = søknad.getMottattDato().equals(lagretMottattDato) ? null : lagretMottattDato;
            var dtoMottattDato = dto.getAnsesMottattDato();

            if (!dtoMottattDato.equals(tidligereAnseesMottattDato)) {
                builder.addLinje(fraTilEquals("Mottatt dato", tidligereAnseesMottattDato, dtoMottattDato));
            }
        }
        builder.addLinje(dto.getBegrunnelse());
        return builder.build();
    }

    private void lagreResultat(Long behandlingId, VurderSøknadsfristDto dto, SøknadEntitet søknad) {
        var mottattDato = dto.harGyldigGrunn() ? dto.getAnsesMottattDato() : søknad.getMottattDato();
        var uttaksperiodegrense = new Uttaksperiodegrense(mottattDato);
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);
    }
}
