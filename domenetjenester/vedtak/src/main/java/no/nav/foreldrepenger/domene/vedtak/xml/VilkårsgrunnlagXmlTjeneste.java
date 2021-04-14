package no.nav.foreldrepenger.domene.vedtak.xml;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.inngangsvilkaar.impl.VilkårJsonObjectMapper;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.vedtak.felles.xml.vedtak.v2.Vilkaar;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.Vilkaarsgrunnlag;

public abstract class VilkårsgrunnlagXmlTjeneste {

    private ObjectFactory vilkårObjectFactory = new ObjectFactory();
    private VilkårJsonObjectMapper objectMapper = new VilkårJsonObjectMapper();
    private SøknadRepository søknadRepository;
    protected FamilieHendelseRepository familieHendelseRepository;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    public VilkårsgrunnlagXmlTjeneste() {
        // For CDI
    }

    public VilkårsgrunnlagXmlTjeneste(BehandlingRepositoryProvider repositoryProvider, KompletthetsjekkerProvider kompletthetsjekkerProvider) {
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    protected VilkårJsonObjectMapper getObjectMapper() {
        return objectMapper;
    }


    public void setVilkårsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling, Vilkaar vilkår) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        var vilkaarsgrunnlag = getVilkaarsgrunnlag(behandling, vilkårFraBehandling, søknad); //Må implementeres i hver subklasse

        if (Objects.nonNull(vilkaarsgrunnlag)) {
            var vilkaarsgrunnlag1 = new no.nav.vedtak.felles.xml.vedtak.v2.Vilkaarsgrunnlag();
            vilkaarsgrunnlag1.getAny().add(vilkårObjectFactory.createVilkaarsgrunnlag(vilkaarsgrunnlag));
            vilkår.setVilkaarsgrunnlag(vilkaarsgrunnlag1);
        }
    }

    protected abstract Vilkaarsgrunnlag getVilkaarsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling, Optional<SøknadEntitet> søknad);

    protected boolean erBarnetFødt(Behandling behandling) {
        final var familieHendelseGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());
        return inneholderFødsel(familieHendelseGrunnlag.getOverstyrtVersjon()) || inneholderFødsel(familieHendelseGrunnlag.getBekreftetVersjon()) ||
            inneholderFødsel(Optional.of(familieHendelseGrunnlag.getSøknadVersjon()));
    }

    private boolean inneholderFødsel(Optional<FamilieHendelseEntitet> familieHendelse) {
        return familieHendelse.map(FamilieHendelseEntitet::getType).map(FamilieHendelseType.FØDSEL::equals).orElse(Boolean.FALSE)
            && !familieHendelse.map(FamilieHendelseEntitet::getBarna).orElse(Collections.emptyList()).isEmpty();
    }

    protected boolean erKomplettSøknad(BehandlingReferanse ref) {
        return kompletthetsjekkerProvider.finnKompletthetsjekkerFor(ref.getFagsakYtelseType(), ref.getBehandlingType()).erForsendelsesgrunnlagKomplett(ref);
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
