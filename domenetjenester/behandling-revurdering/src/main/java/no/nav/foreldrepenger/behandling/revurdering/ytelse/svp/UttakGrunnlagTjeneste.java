package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.YtelsesesspesifiktGrunnlagTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class UttakGrunnlagTjeneste implements YtelsesesspesifiktGrunnlagTjeneste {

    private FamilieHendelseRepository familieHendelseRepository;
    private SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    public UttakGrunnlagTjeneste(FamilieHendelseRepository familieHendelseRepository, SvangerskapspengerRepository svangerskapspengerRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
    }

    UttakGrunnlagTjeneste() {
        // CDI
    }

    @Override
    public Optional<SvangerskapspengerGrunnlag> grunnlag(BehandlingReferanse ref) {
        var familieHendelse = familieHendelse(ref);
        if (familieHendelse.isEmpty()) {
            return Optional.empty();
        }
        SvangerskapspengerGrunnlag grunnlag = new SvangerskapspengerGrunnlag()
                .medFamilieHendelse(familieHendelse.get())
                .medSvpGrunnlagEntitet(svpGrunnEntitet(ref).orElse(null));
        return Optional.of(grunnlag);
    }

    private Optional<SvpGrunnlagEntitet> svpGrunnEntitet(BehandlingReferanse ref) {
        return svangerskapspengerRepository.hentGrunnlag(ref.getBehandlingId());
    }

    private Optional<FamilieHendelse> familieHendelse(BehandlingReferanse ref) {
        var gjeldendeFamiliehendelseOpt = familieHendelseRepository.hentAggregatHvisEksisterer(ref.getBehandlingId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        if (gjeldendeFamiliehendelseOpt.isEmpty()) {
            return Optional.empty();
        }
        FamilieHendelseEntitet gjeldendeFamiliehendelse = gjeldendeFamiliehendelseOpt.get();
        var terminbekreftelse = gjeldendeFamiliehendelse.getTerminbekreftelse();
        if (terminbekreftelse.isEmpty()) {
            throw new IllegalStateException("Det skal alltid være termindato på svangerskapspenger søknad.");
        }

        var termindato = terminbekreftelse.get().getTermindato();
        var fødselsdatoOptional = gjeldendeFamiliehendelse.getFødselsdato();
        var barna = gjeldendeFamiliehendelse.getBarna().stream().map(ib -> new Barn(ib.getDødsdato().orElse(null))).collect(Collectors.toList());
        return Optional.of(FamilieHendelse.forFødsel(termindato, fødselsdatoOptional.orElse(null), barna, gjeldendeFamiliehendelse.getAntallBarn()));
    }
}
