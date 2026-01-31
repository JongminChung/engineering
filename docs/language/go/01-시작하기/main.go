package main

import "fmt"

func main() {
	var name string
	var age int
	var learning bool

	fmt.Printf("zero values: name=%q age=%d learning=%t\n", name, age, learning)

	var ptr *int
	var numbers []int
	var lookup map[string]int
	var stream chan int
	var anything interface{}

	fmt.Printf("nil states: ptr=%v slice=%v map=%v chan=%v iface=%v\n", ptr, numbers, lookup, stream, anything)

	array := [3]int{1, 2, 3}
	arrayCopy := array
	arrayCopy[0] = 9

	slice := []int{1, 2, 3}
	sliceCopy := slice
	sliceCopy[0] = 9
	slice = append(slice, 4)

	fmt.Printf("array=%v arrayCopy=%v\n", array, arrayCopy)
	fmt.Printf("slice=%v sliceCopy=%v\n", slice, sliceCopy)

	// 미리 용량을 알면 한 번에 할당 (재할당 0회)
	type User struct {
		Id int
	}
	// make([]타입, 길이, 용량)
	data := make([]User, 0, 1000)

	for i := 0; i < 1000; i++ {
		data = append(data, User{Id: i})
	}

	fmt.Printf("data=%v\n", data)

	// for-in
	words := []string{"go", "java", "rust"}
	fmt.Println("range loop (for-in)")
	for i, word := range words {
		fmt.Printf("%d:%s ", i, word)
	}
	fmt.Println()

	// repeat-while
	repeat := 0
	for repeat < 2 {
		repeat++
		fmt.Printf("repeat-while step=%d\n", repeat)
	}

	// return multiple values
	// var userName string
	// var userAge int
	// `:=`: 초기화 + 선언
	// 모두 선언했다면 `:=`로 못 사용함 (컴파일 에러)
	userName, userAge := nameAndAge(1)
	fmt.Printf("name=%s age=%d\n", userName, userAge)

	// function pointer
	fmt.Println("runMathOp (add)", runMathOp(1, 2, add))
	fmt.Println("runMathOp (sub)", runMathOp(1, 2, sub))

	ints := []int{1, 2, 3}
	floats := []float64{1.5, 2.5, 3.0}
	fmt.Printf("sum ints=%d\n", Sum(ints))
	fmt.Printf("sum floats=%.1f\n", Sum(floats))

	profile := Profile{Name: "Gopher", Age: 2}
	profilePtr := &Profile{Name: "Gopher", Age: 3}
	profilePtr.Age++
	fmt.Printf("profile=%s ptr=%s\n", profile.Greeting(), profilePtr.Greeting())

	counter := Counter{Value: 1}
	counterPtr := &counter
	counterPtr.Value += 2
	newCounterPtr := newCounter(5)
	var emptyCounter *Counter
	fmt.Printf("counter=%d new=%d nil=%v\n", counter.Value, newCounterPtr.Value, emptyCounter)

	// defer
	deferExample()
	deferExample2(5)

	// generic
	fmt.Println(Sum([]int{1, 2, 3}))
	fmt.Println(Sum([]float64{1.5, 2.5}))
}

func valueOfPi(multiplier uint) float32 {
	return 3.14159 * float32(multiplier)
}

func nameAndAge(uid int) (name string, age int) {
	return "go", uid * 10
}

func runMathOp(a int, b int, op func(int, int) int) int {
	return op(a, b)
}

func add(a int, b int) int {
	return a + b
}

func sub(a int, b int) int {
	return a - b
}

// The deferred call's arguments are evaluated immediately, but the function call is not executed until the surrounding function returns."
func deferExample() {
	str := "deferred"
	defer fmt.Println(str) // 이 순간 str 값이 캡처됨 ("deferred")

	str += " world"
	fmt.Println("hello")
}

func deferExample2(x int) int {
	defer func() {
		fmt.Println("This is being called from an inline function")
		fmt.Println("I can put multiple statements in here")

		z := x - 1
		fmt.Println("z", z)
	}()

	y := x + 1
	fmt.Println("y", y)

	return y
}

// generic
type Number interface {
	~int | ~float64
}

type Profile struct {
	Name string
	Age  int
}

type Counter struct {
	Value int
}

func Sum[T Number](values []T) T {
	var total T
	for _, v := range values {
		total += v
	}
	return total
}

func (p Profile) Greeting() string {
	return fmt.Sprintf("%s(%d)", p.Name, p.Age)
}

func newCounter(value int) *Counter {
	return &Counter{Value: value}
}
