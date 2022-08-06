package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.Søknadsfrister;

public abstract class VurderSøknadsfristOppdatererTjeneste {

    private HistorikkTjenesteAdapter historikkAdapter;
    private SøknadRepository søknadRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    public VurderSøknadsfristOppdatererTjeneste(HistorikkTjenesteAdapter historikkAdapter,
                                                BehandlingRepositoryProvider repositoryProvider) {
        this.historikkAdapter = historikkAdapter;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
    }

    VurderSøknadsfristOppdatererTjeneste() {
        //CDI
    }

    public OppdateringResultat oppdater(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var søknad = søknadRepository.hentSøknad(behandlingId);

        opprettHistorikkinnslag(dto, param, søknad);
        lagreResultat(behandlingId, dto, søknad);

        return OppdateringResultat.utenOveropp();
    }

    private void lagreResultat(Long behandlingId, VurderSøknadsfristDto dto, SøknadEntitet søknad) {
        var mottattDato = dto.harGyldigGrunn() ? dto.getAnsesMottattDato() : søknad.getMottattDato();
        var førsteLovligeUttaksdag = Søknadsfrister.tidligsteDatoDagytelse(mottattDato);
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
            .medMottattDato(mottattDato)
            .medFørsteLovligeUttaksdag(førsteLovligeUttaksdag)
            .build();
        uttaksperiodegrenseRepository.lagre(behandlingId, uttaksperiodegrense);
        lagreYtelseSpesifikkeData(behandlingId, uttaksperiodegrense);
    }

    protected abstract void lagreYtelseSpesifikkeData(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense);

    private void opprettHistorikkinnslag(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param, SøknadEntitet søknad) {
        var tekstBuilder = historikkAdapter.tekstBuilder()
            .medSkjermlenke(SkjermlenkeType.SOEKNADSFRIST)
            .medEndretFelt(HistorikkEndretFeltType.SOKNADSFRIST, null,
                dto.harGyldigGrunn() ? HistorikkEndretFeltVerdiType.HAR_GYLDIG_GRUNN : HistorikkEndretFeltVerdiType.HAR_IKKE_GYLDIG_GRUNN)
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret());

        if (dto.harGyldigGrunn()) {
            var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(param.getBehandlingId());
            var lagretMottattDato = uttaksperiodegrense.getMottattDato();
            var tidligereAnseesMottattDato = søknad.getMottattDato().equals(lagretMottattDato) ? null : lagretMottattDato;
            var dtoMottattDato = dto.getAnsesMottattDato();

            if (!dtoMottattDato.equals(tidligereAnseesMottattDato)) {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.MOTTATT_DATO, tidligereAnseesMottattDato, dtoMottattDato);
            }
        }
    }
}
