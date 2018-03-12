	.text
	.globl main
putchar:
	pushq %rbp
	movq %rsp, %rbp
	popq %rbp
	ret
sbrk:
	pushq %rbp
	movq %rsp, %rbp
	popq %rbp
	ret
fact_rec:
	pushq %rbp
	movq %rsp, %rbp
	addq $-8, %rsp
	movq %rbx, -8(%rbp)
	movq %rdi, %r10
	movq $1, %rax
	cmpq %rax, %r10
	setle %r10
	testq $0, %r10
	jnz L12
	movq %rdi, %rbx
	movq $1, %rax
	subq %rax, %rdi
	call fact_rec
	imulq %rax, %rbx
L3:
	movq %rbx, %rax
	movq -8(%rbp), %rbx
	addq $8, %rsp
	popq %rbp
	ret
	movq $1, %rbx
	jmp L3
main:
	pushq %rbp
	movq %rsp, %rbp
	movq $10, %rdi
	call fact_rec
	popq %rbp
	ret
	.data
