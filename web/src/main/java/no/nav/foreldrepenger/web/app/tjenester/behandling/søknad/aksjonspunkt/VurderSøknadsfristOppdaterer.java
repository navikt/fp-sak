package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderSøknadsfristDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderSøknadsfristOppdaterer implements AksjonspunktOppdaterer<VurderSøknadsfristDto> {

    private Historikkinnslag2Repository historikkinnslag2Repository;
    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    VurderSøknadsfristOppdaterer() {
    }

    @Inject
    public VurderSøknadsfristOppdaterer(Historikkinnslag2Repository historikkinnslag2Repository, BehandlingRepositoryProvider repositoryProvider) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    @Override
    public OppdateringResultat oppdater(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var søknad = søknadRepository.hentSøknad(behandlingId);

        var historikkinnslag = lagHistorikkinnslag(dto, param, søknad);
        historikkinnslag2Repository.lagre(historikkinnslag);
        lagreResultat(behandlingId, dto, søknad);
        return OppdateringResultat.utenOverhopp();
    }

    private Historikkinnslag2 lagHistorikkinnslag(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param, SøknadEntitet søknad) {
        var builder = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getFagsakId())
            .medTittel(SkjermlenkeType.SOEKNADSFRIST)
            .addTekstlinje(fraTilEquals("Søknadsfrist", null,  dto.harGyldigGrunn()
                ? HistorikkEndretFeltVerdiType.HAR_GYLDIG_GRUNN.getNavn()
                : HistorikkEndretFeltVerdiType.HAR_IKKE_GYLDIG_GRUNN.getNavn()))
            .addTekstlinje(dto.getBegrunnelse());

        if (dto.harGyldigGrunn()) {
            var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(param.getBehandlingId());
            var lagretMottattDato = uttaksperiodegrense.getMottattDato();
            var tidligereAnseesMottattDato = søknad.getMottattDato().equals(lagretMottattDato) ? null : lagretMottattDato;
            var dtoMottattDato = dto.getAnsesMottattDato();

            if (!dtoMottattDato.equals(tidligereAnseesMottattDato)) {
                builder.addTekstlinje(fraTilEquals("Mottatt dato", tidligereAnseesMottattDato, dtoMottattDato));
            }
        }
        return builder.build();
    }

    private void lagreResultat(Long behandlingId, VurderSøknadsfristDto dto, SøknadEntitet søknad) {
        var mottattDato = dto.harGyldigGrunn() ? dto.getAnsesMottattDato() : søknad.getMottattDato();
        var uttaksperiodegrense = new Uttaksperiodegrense(mottattDato);
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);
    }
}
