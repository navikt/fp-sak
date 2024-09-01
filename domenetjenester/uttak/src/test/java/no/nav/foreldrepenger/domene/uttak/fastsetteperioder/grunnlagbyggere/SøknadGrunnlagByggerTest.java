package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.DokumentasjonVurdering.MORS_AKTIVITET_GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;

class SøknadGrunnlagByggerTest {

    @Test
    void byggerSøknadsperioder() {
        var repositoryProvider = new UttakRepositoryStubProvider();
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var søknadGrunnlagBygger = new SøknadGrunnlagBygger(ytelsesFordelingRepository);

        var fom = LocalDate.of(2020, 12, 12);
        var tom = LocalDate.of(2020, 12, 13);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medPeriode(fom, tom)
            .medDokumentasjonVurdering(new DokumentasjonVurdering(DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT))
            .build();
        var behandling = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .lagre(repositoryProvider);
        var ytelsespesifiktGrunnlag = fpGrunnlag(fom);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null, ytelsespesifiktGrunnlag);
        var grunnlag = søknadGrunnlagBygger.byggGrunnlag(input).build();

        assertThat(grunnlag.getOppgittePerioder()).hasSize(1);
        assertThat(grunnlag.getOppgittePerioder().get(0).getStønadskontotype()).isEqualTo(Stønadskontotype.FELLESPERIODE);
        assertThat(grunnlag.getOppgittePerioder().get(0).getFom()).isEqualTo(fom);
        assertThat(grunnlag.getOppgittePerioder().get(0).getTom()).isEqualTo(tom);
        assertThat(grunnlag.getOppgittePerioder().get(0).getDokumentasjonVurdering()).isEqualTo(MORS_AKTIVITET_GODKJENT);
    }

    private ForeldrepengerGrunnlag fpGrunnlag(LocalDate fødselsdato) {
        return new ForeldrepengerGrunnlag().medFamilieHendelser(
            new FamilieHendelser().medSøknadHendelse(FamilieHendelse.forFødsel(fødselsdato, fødselsdato, List.of(), 1)));
    }
}
