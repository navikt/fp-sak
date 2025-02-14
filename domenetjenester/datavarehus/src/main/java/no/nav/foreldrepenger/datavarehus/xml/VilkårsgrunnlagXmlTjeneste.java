package no.nav.foreldrepenger.datavarehus.xml;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.vedtak.felles.xml.vedtak.v2.Vilkaar;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.Vilkaarsgrunnlag;

public abstract class VilkårsgrunnlagXmlTjeneste {

    private ObjectFactory vilkårObjectFactory = new ObjectFactory();
    private SøknadRepository søknadRepository;
    private Kompletthetsjekker kompletthetsjekker;
    protected FamilieHendelseRepository familieHendelseRepository;

    public VilkårsgrunnlagXmlTjeneste() {
        // For CDI
    }

    public VilkårsgrunnlagXmlTjeneste(BehandlingRepositoryProvider repositoryProvider, Kompletthetsjekker kompletthetsjekker) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }


    public void setVilkårsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling, Vilkaar vilkår) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        var familieHendelseDato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getSkjæringstidspunkt);
        var vilkaarsgrunnlag = getVilkaarsgrunnlag(behandling, vilkårFraBehandling, søknad, familieHendelseDato); //Må implementeres i hver subklasse

        if (Objects.nonNull(vilkaarsgrunnlag)) {
            var vilkaarsgrunnlag1 = new no.nav.vedtak.felles.xml.vedtak.v2.Vilkaarsgrunnlag();
            vilkaarsgrunnlag1.getAny().add(vilkårObjectFactory.createVilkaarsgrunnlag(vilkaarsgrunnlag));
            vilkår.setVilkaarsgrunnlag(vilkaarsgrunnlag1);
        }
    }

    protected abstract Vilkaarsgrunnlag getVilkaarsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling, Optional<SøknadEntitet> søknad, Optional<LocalDate> familieHendelseDato);

    protected boolean erBarnetFødt(Behandling behandling) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        return inneholderFødsel(familieHendelseGrunnlag.getOverstyrtVersjon()) || inneholderFødsel(familieHendelseGrunnlag.getBekreftetVersjon()) ||
            inneholderFødsel(Optional.of(familieHendelseGrunnlag.getSøknadVersjon()));
    }

    private boolean inneholderFødsel(Optional<FamilieHendelseEntitet> familieHendelse) {
        return familieHendelse.map(FamilieHendelseEntitet::getType).map(FamilieHendelseType.FØDSEL::equals).orElse(Boolean.FALSE)
            && !familieHendelse.map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList()).isEmpty();
    }

    protected boolean erKomplettSøknad(BehandlingReferanse ref) {
        return kompletthetsjekker.erForsendelsesgrunnlagKomplett(ref);
    }

    protected LocalDate getMottattDato(Behandling behandling) {
        var søknadOptional = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOptional.isPresent()) {
            var søknad = søknadOptional.get();
            return søknad.getMottattDato();
        }
        return null;
    }

}
